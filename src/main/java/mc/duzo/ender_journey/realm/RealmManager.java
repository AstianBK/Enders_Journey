package mc.duzo.ender_journey.realm;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3d;
import com.supermartijn642.movingelevators.blocks.ControllerBlockEntity;
import com.supermartijn642.movingelevators.elevator.ElevatorGroup;
import com.supermartijn642.movingelevators.elevator.ElevatorGroupCapability;
import eu.asangarin.meaddon.block.CustomRemoteControllerBlockEntity;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.block.BteAbstractWorkBlock;
import fr.shoqapik.btemobs.block.ExplorerTableBlock;
import fr.shoqapik.btemobs.entity.*;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import fr.shoqapik.btemobs.registry.BteMobsEntities;
import mc.duzo.ender_journey.CommonProxy;
import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.common.DimensionUtil;
import mc.duzo.ender_journey.common.block_entity.ColumnBlockEntity;
import mc.duzo.ender_journey.common.blocks.TheNewEndPortalBlock;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.data.Savable;
import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.contents.Contents;
import twilightforest.block.FireflyBlock;
import vazkii.quark.base.Quark;
import vazkii.quark.base.block.QuarkBlock;
import vazkii.quark.base.block.QuarkBlockWrapper;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.processBlockInfos;

public class RealmManager implements Savable {
	private Structure structure;
	private PlayerManager player;

	public RealmManager() {}
	public RealmManager(CompoundTag data) {
		this.deserialise(data);
	}

	@Override
	public CompoundTag serialise() {
		CompoundTag data = new CompoundTag();

		data.put("Structure", this.getStructure().serialise());
		data.put("Player", this.getPlayer().serialise());

		return data;
	}

	@Override
	public void deserialise(CompoundTag data) {
		this.structure = new Structure(data.getCompound("Structure"));
		this.player = new PlayerManager(this, data.getCompound("Player"));
	}

	public static boolean isInRealm(LivingEntity entity) {
		return EnderDimensions.isInDimension(entity, EnderDimensions.REALM_KEY);
	}
	public static ServerLevel getDimension() {
		return EndersJourney.getServer().getLevel(EnderDimensions.REALM_KEY);
	}

	public Structure getStructure() {
		if (this.structure == null) {
			EndersJourney.LOGGER.warn("Missing realm structure! Creating..");
			this.structure = new Structure();
		}

		return this.structure;
	}

	public PlayerManager getPlayer() {
		if (this.player == null) {
			EndersJourney.LOGGER.warn("Missing player manager! Creating..");
			this.player = new PlayerManager(this);
		}

		return this.player;
	}

	public void teleport(LivingEntity entity) {
		Vector3d vec = EndersJourney.getCentre(this.getStructure().getCentre());
		EnderDimensions.teleport(entity, getDimension(), vec, entity.getYRot(), entity.getXRot());
	}

	public static class Structure implements Savable {

		private final ResourceLocation structure;
		private boolean isPlaced;
		private boolean plataformPlaced;
		private BlockPos centre;
		private Map<String, BlockPos> dimensionTp = new HashMap<>();

		private final int[] posElevator = new int[]{1, -1};
		private final List<BlockPos> posElevators = List.of(
				new BlockPos(0, 53, -38),
				new BlockPos(0, 80, -38),
				new BlockPos(0, 112, -38)
		);
		private final Queue<Runnable> tasks = new ArrayDeque<>();
		private boolean generating = false;
		private static final int TASKS_PER_TICK = 2;

		public Structure(ResourceLocation structure, @Nullable BlockPos centre) {
			this.structure = structure;
			this.isPlaced = false;
			this.plataformPlaced = false;
			this.centre = centre;
		}

		public Structure() {
			this(getDefaultStructure(), new BlockPos(0, 143, 0));
		}

		public Structure(CompoundTag data) {
			this.structure = new ResourceLocation(data.getString("Structure"));
			this.deserialise(data);
		}

		public void setTpPosForDimension(BlockPos pos,String dimension){
			if(dimensionTp.containsKey(dimension)){
				Map<String,BlockPos> var = new HashMap<>();
				for (Map.Entry<String,BlockPos> entry : dimensionTp.entrySet()){
					if(!dimension.equals(entry.getKey())){
						var.put(entry.getKey(),entry.getValue());
					}
				}
				var.put(dimension,pos);
				dimensionTp = var;
			}else {
				dimensionTp.put(dimension,pos);
			}
		}

		public BlockPos getTpPosForDimension(String dimension){
			return this.dimensionTp.containsKey(dimension) ? this.dimensionTp.get(dimension) : null;
		}

		@Override
		public CompoundTag serialise() {
			CompoundTag data = new CompoundTag();

			data.putString("Structure", this.structure.toString());
			data.putBoolean("plataformPlaced", this.plataformPlaced);
			data.putBoolean("isPlaced", this.isPlaced);

			if (this.centre != null)
				data.put("Centre", NbtUtils.writeBlockPos(this.centre));

			if (!this.dimensionTp.isEmpty()) {
				ListTag tags = new ListTag();
				this.dimensionTp.forEach((s, pos) -> {
					CompoundTag nbt = new CompoundTag();
					nbt.put("pos", NbtUtils.writeBlockPos(pos));
					nbt.putString("key", s);
					tags.add(nbt);
				});
				data.put("dimensionTps", tags);
			}

			return data;
		}

		@Override
		public void deserialise(CompoundTag data) {
			this.isPlaced = data.getBoolean("isPlaced");
			this.plataformPlaced = data.getBoolean("plataformPlaced");

			if (data.contains("Centre"))
				this.centre = NbtUtils.readBlockPos(data.getCompound("Centre"));

			if (data.contains("dimensionTps")) {
				Map<String, BlockPos> var = new HashMap<>();
				ListTag tags = data.getList("dimensionTps", 10);

				for (int i = 0; i < tags.size(); i++) {
					CompoundTag tag = tags.getCompound(i);
					var.put(tag.getString("key"), NbtUtils.readBlockPos(tag.getCompound("pos")));
				}

				this.dimensionTp = var;
			}
		}

		public void placeComponentFull(ServerLevel level, int addX, int height, int addZ, ResourceLocation location,StructurePlaceSettings settings){
			StructureTemplate template = this.findStructure(location).orElse(null);
			if (template == null) return;

			Vec3i size = template.getSize();
			BlockPos origin = new BlockPos(-size.getX() / 2 + addX, height, -size.getZ() / 2 + addZ);

			template.placeInWorld(level,origin,origin,settings,level.random,2);
		}

		public void placeComponent(ServerLevel level, int addX, int height, int addZ, ResourceLocation location,StructurePlaceSettings settings) {
			StructureTemplate template = this.findStructure(location).orElse(null);
			if (template == null) return;

			List<StructureTemplate.StructureBlockInfo> blocks = settings.getRandomPalette(template.palettes, BlockPos.ZERO).blocks();

			Vec3i size = template.getSize();
			BlockPos origin = new BlockPos(-size.getX() / 2 + addX, height, -size.getZ() / 2 + addZ);

			List<StructureTemplate.StructureBlockInfo> placed = new ArrayList<>();

			for (StructureTemplate.StructureBlockInfo info : blocks) {
				if(info.state.isAir())continue;
				BlockPos finalPos = origin.offset(info.pos);
				placed.add(new StructureTemplate.StructureBlockInfo(finalPos, info.state, info.nbt));
			}
			int batchSize = 500;

			for (int i = 0; i < placed.size(); i += batchSize) {
				int start = i;

				tasks.add(() -> {
					for (int j = start; j < start + batchSize && j < placed.size(); j++) {
						StructureTemplate.StructureBlockInfo info = placed.get(j);


						level.setBlock(info.pos, info.state, 3);

						if (info.nbt != null) {
							BlockEntity be = level.getBlockEntity(info.pos);
							if (be != null) {
								be.load(info.nbt);
							}
						}
					}
				});
			}
		}
		public boolean isPlaced() {
			return this.isPlaced;
		}

		private static ResourceLocation getDefaultStructure() {
			return new ResourceLocation(EndersJourney.MODID, "temple");
		}

		private Optional<StructureTemplate> findStructure(ResourceLocation structure) {
			return EndersJourney.getServer().getStructureManager().get(structure);
		}

		public void verify() {
			if (!this.isPlaced && !generating) {
				CommonProxy.initBlocks();
				this.place();
			}
		}

		private void place() {
			this.place(getDimension(), true);
		}

		private void place(ServerLevel level, boolean inform) {
			if (level == null ) return;

			this.isPlaced = true;


			long start = System.currentTimeMillis();

			enqueueGeneration(level, start);
		}

		public void tick() {

			for (int i = 0; i < TASKS_PER_TICK; i++) {
				Runnable task = tasks.poll();
				if (task == null) {
					return;
				}
				task.run();
			}
		}

		public void settingElevator(ServerLevel level){
			ElevatorGroupCapability cap = ElevatorGroupCapability.get(level);
			boolean isFirstConfig = true;

			for (BlockPos pos : this.posElevators) {

				ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(pos);
				if (controller == null) continue;

				cap.add(controller);

				for (int i = 0; i < 2; i++) {
					BlockPos relativePos = pos.offset(this.posElevator[i], 1, 1);
					CustomRemoteControllerBlockEntity remote =
							(CustomRemoteControllerBlockEntity) level.getBlockEntity(relativePos);

					if (remote != null) {
						remote.setCamoState(Blocks.BLACKSTONE.defaultBlockState());
						remote.setValues(remote.getFacing(), pos, controller.getFacing());
					}
				}

				for (int i = 0; i < 2; i++) {
					BlockPos relativePos = pos.offset(this.posElevator[i] * 3, 1, isFirstConfig ? 9 : 10);
					CustomRemoteControllerBlockEntity remote =
							(CustomRemoteControllerBlockEntity) level.getBlockEntity(relativePos);

					if (remote != null) {
						remote.setCamoState(Blocks.BLACKSTONE.defaultBlockState());
						remote.setValues(remote.getFacing(), pos, controller.getFacing());
					}
				}

				if (isFirstConfig) {
					ElevatorGroup data = cap.get(0, -38, controller.getFacing());
					if (data != null) {
						data.increaseCageDepthOffset();
						data.setTargetSpeed(0.8F);

						for (int i = 0; i < 4; i++) {
							data.increaseCageDepth();
							data.increaseCageWidth();
						}

						for (int j = 0; j < 3; j++) {
							data.decreaseCageHeight();
						}
					}
					isFirstConfig = false;
				}
			}
		}

		private void placeColumn(ServerLevel level) {
			DimensionUtil.eyeItemForBlockPos.forEach((itemStack, pos) -> {
				ColumnBlockEntity block = new ColumnBlockEntity(pos, BKBlocks.COLUMN.get().defaultBlockState());
				level.setBlock(pos, block.getBlockState(), 3);

				ColumnBlockEntity blockEntity = (ColumnBlockEntity) level.getBlockEntity(pos);
				if (blockEntity != null) {
					blockEntity.setItem(DimensionUtil.getItem(itemStack));
				}
			});

			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9, 50, -10), new BlockPos(9, 50, 10))) {
				level.setBlock(pos, Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
			}

			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9, 51, -10), new BlockPos(9, 51, 10))) {
				if (level.isEmptyBlock(pos)) {
					level.setBlock(pos, BKBlocks.THE_NEW_END_PORTAL.get().defaultBlockState(), 3);
				}
			}

			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(33, 94, -3), new BlockPos(33, 86, 3))) {
				level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
			}

			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-32, 94, 3), new BlockPos(-32, 85, -4))) {
				level.setBlock(pos, Blocks.NETHER_BRICKS.defaultBlockState(), 3);
			}
		}
		private void enqueueGeneration(ServerLevel level, long start) {
			makeSuperiorIsland(level, start);
			makeInitialIsland(level, start);
			makeInitialRoom(level, start);
			makePortals(level, start);


			tasks.add(()->settingElevator(level));
			placeColumn(level);

			checkAndGeneratePlatform(level);
			tasks.add(()->{
				if (ModList.get().isLoaded("bte_mobs")) {
					spawnNpc5(level);
					spawnBlackSmith(level);
					spawnDruid(level);
					spawnWarlock(level);
					spawnExplorer(level);
				}
			});
			placeSign(start, level);
		}


		public void checkAndGeneratePlatform(ServerLevel level) {
			if (plataformPlaced) return;

			if (!CommonProxy.BLOCKS.isEmpty()) {
				for (CommonProxy.PlacedBlock placed : CommonProxy.BLOCKS) {
					BlockState state = placed.block().defaultBlockState();

					if (placed.facing() != null &&
							(state.hasProperty(HorizontalDirectionalBlock.FACING) || state.hasProperty(BlockStateProperties.FACING))) {

						if (state.is(Contents.RITO_GODDESS_STATUE.get())) {
							state = state.setValue(HorizontalDirectionalBlock.FACING, placed.facing());
						} else {
							state = state.setValue(BlockStateProperties.FACING, placed.facing());
						}
					}

					level.setBlock(placed.pos(), state, 3);
				}
				plataformPlaced = true;
			}
		}

		public void spawnExplorer(ServerLevel level) {
			BlockPos pos = new BlockPos(3, 132, -69);
			ExplorerEntity entity = new ExplorerEntity(BteMobsEntities.EXPLORER_ENTITY.get(), level);
			entity.setPos(3.5F, 133, -69.5F);
			entity.setTablePos(pos);
			entity.setAttachFace(Direction.SOUTH);
			level.addFreshEntity(entity);

			BlockState table = BteMobsBlocks.EXPLORER_TABLE.get().defaultBlockState()
					.setValue(ExplorerTableBlock.FACING, Direction.SOUTH);

			level.setBlock(pos, table, 3);
		}

		public void spawnDruid(ServerLevel level) {
			BlockPos pos = new BlockPos(-3, 129, 54);
			DruidEntity entity = new DruidEntity(BteMobsEntities.DRUID_ENTITY.get(), level);
			entity.setPos(-2.5F, 129, 53.5F);
			entity.setAttachFace(Direction.NORTH);
			entity.setYRot(Direction.NORTH.toYRot());
			level.addFreshEntity(entity);

			level.setBlock(pos, BteMobsBlocks.ORIANA_OAK.get().defaultBlockState(), 3);
			entity.tablePos = pos;
		}

		public void spawnNpc5(ServerLevel level) {
			Npc5Entity entity = new Npc5Entity(BteMobsEntities.NPC5_ENTITY.get(), level);
			entity.setPos(0.5F, 80, 0.5F);
            entity.setYRot(Direction.NORTH.toYRot());
			level.addFreshEntity(entity);
			level.broadcastEntityEvent(entity,(byte) 5);
		}

		public void spawnWarlock(ServerLevel level) {
			WarlockEntity entity = new WarlockEntity(BteMobsEntities.WARLOCK_ENTITY.get(), level);
			entity.setPos(-62.5F, 134, 0.5F);
			entity.setAttachFace(Direction.EAST);
			entity.setYRot(Direction.EAST.toYRot());
			level.addFreshEntity(entity);
		}

		public void spawnBlackSmith(ServerLevel level) {
			Entity entity = new BlacksmithEntity(BteMobsEntities.BLACKSMITH_ENTITY.get(), level);
			entity.setPos(62.5F, 134, 0.5F);
			entity.setYBodyRot(Direction.WEST.toYRot());
			level.addFreshEntity(entity);

			BlockState magmaBlock = BteMobsBlocks.MAGMA_FORGE.get().defaultBlockState()
					.setValue(BteAbstractWorkBlock.FACING, Direction.EAST);

			level.setBlock(new BlockPos(61, 133, 0), magmaBlock, 3);
			level.setBlock(new BlockPos(61, 133, 1), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(new BlockPos(61, 133, -1), Blocks.AIR.defaultBlockState(), 3);
		}

		public void placeSign(long start, ServerLevel level) {
			StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_180);

			placeComponent( level, 11, 119, 0, new ResourceLocation(EndersJourney.MODID, "cartel_cognition"), settings);
			placeComponent( level, -11, 119, 0, new ResourceLocation(EndersJourney.MODID, "cartel_waystone"), settings);
			placeComponent( level, 0, 119, 11, new ResourceLocation(EndersJourney.MODID, "cartel_ocultismo"), settings);
		}


		public void makeInitialIsland(ServerLevel level, long start) {
			placeComponent( level, 10, -1, 0, new ResourceLocation(EndersJourney.MODID, "island_inferior"), new StructurePlaceSettings());
		}

		public void makeInitialRoom(ServerLevel level, long start) {
			placeComponent( level, -4, 48, -4, new ResourceLocation(EndersJourney.MODID, "island_center"), new StructurePlaceSettings());
		}

		public void makePortals(ServerLevel level, long start) {
			placeComponent( level, -7, 78, 3, new ResourceLocation(EndersJourney.MODID, "island_superior"), new StructurePlaceSettings());
		}

		public void makeSuperiorIsland(ServerLevel level, long start) {
			placeComponentFull( level, 5, 111, -3, new ResourceLocation(EndersJourney.MODID, "temple"), new StructurePlaceSettings());
		}

		public BlockPos getCentre() {
			this.verify();
			if (this.centre != null) return this.centre;

			this.centre = new BlockPos(0, 143, 0);
			return this.centre;
		}

		@Override
		public String toString() {
			return "RealmStructure{" +
					"structure=" + structure +
					", isPlaced=" + isPlaced +
					", centre=" + centre +
					'}';
		}
	}


	public static class PlayerManager implements Savable {
		private HashMap<UUID, RealmPlayer> seen; // All players this manager has seen before
		private final RealmManager parent;

		public PlayerManager(RealmManager parent) {
			this.parent = parent;
			this.seen = new HashMap<>();
		}

		public PlayerManager(RealmManager parent, CompoundTag data) {
			this(parent);

			this.deserialise(data);
		}

		public boolean hasSeen(RealmPlayer player) {
			return this.hasSeen(player.id);
		}
		public boolean hasSeen(Player player) {
			return this.hasSeen(player.getUUID());
		}
		public boolean hasSeen(UUID id) {
			return this.seen.containsKey(id);
		}

		public void onJoin(Player player) {
			if (player instanceof ServerPlayer serverPlayer) {
				this.runSpawnLogic(serverPlayer);
			}
		}
		public void onLeave(Player player) {

		}
		public void onRespawn(Player player) {
			if (player instanceof ServerPlayer) {
				this.runSpawnLogic((ServerPlayer) player);
			}
		}


		private void runSpawnLogic(ServerPlayer player) {
			if (this.hasSeen(player)) {
				// Assume we don´t need to adjust their spawnpoint + teleport.
				return;
			}
			if (this.parent.getStructure().getCentre() == null && getDimension()==null) return;

			this.parent.teleport(player);
			player.setRespawnPosition(RealmManager.getDimension().dimension(), this.parent.getStructure().getCentre(), 0, true, false);

			this.addPlayer(player);
		}

		private void addPlayer(ServerPlayer player) {
			this.seen.put(player.getUUID(), new RealmPlayer(player));
		}

		private void verifySeen(ServerPlayer player) {
			if (!this.hasSeen(player)) this.runSpawnLogic(player);
		}
		private void verifyAll() {
			long start = System.currentTimeMillis();

			EndersJourney.getServer().getPlayerList().getPlayers().forEach(this::verifySeen);

			EndersJourney.LOGGER.info("Verified all players in " + (System.currentTimeMillis() - start) + "ms");
		}
		public RealmPlayer getPlayerForUUID(UUID uuid){
			return this.seen.get(uuid);
		}

		@Override
		public CompoundTag serialise() {
			CompoundTag data = new CompoundTag();

			CompoundTag seenData = new CompoundTag();
			this.seen.forEach((uuid, realmPlayer) -> seenData.put(uuid.toString(), realmPlayer.serialise()));
			data.put("Seen", seenData);

			return data;
		}

		@Override
		public void deserialise(CompoundTag data) {
			this.seen = new HashMap<>();
			CompoundTag seenData = data.getCompound("Seen");

			seenData.getAllKeys().forEach(key -> this.seen.put(UUID.fromString(key), new RealmPlayer(UUID.fromString(key),seenData.getCompound(key))));
		}
	}

	public static class RealmPlayer implements Savable { // unnecessary now
		private final UUID id;
		private ServerPlayer playerCache;
		private boolean firstTp = true;
		private BlockPos tpPos = null;
		public RealmPlayer(UUID id) {
			this.id = id;
		}
		public RealmPlayer(ServerPlayer player) {
			this(player.getUUID());

			this.playerCache = player;
		}
		public RealmPlayer(UUID uuid,CompoundTag data) {
			this(uuid);
			this.deserialise(data);
		}

		public ServerPlayer asPlayer() {
			if (this.playerCache != null) return this.playerCache;

			return EndersJourney.getServer().getPlayerList().getPlayer(this.id);
		}

		@Override
		public CompoundTag serialise() {
			CompoundTag data = new CompoundTag();


			data.putUUID("ID", this.id);
			return data;
		}

		@Override
		public void deserialise(CompoundTag data) {

		}
	}
}
