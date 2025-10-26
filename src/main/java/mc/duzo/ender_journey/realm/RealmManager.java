package mc.duzo.ender_journey.realm;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3d;
import com.supermartijn642.movingelevators.blocks.ControllerBlockEntity;
import com.supermartijn642.movingelevators.elevator.ElevatorGroup;
import com.supermartijn642.movingelevators.elevator.ElevatorGroupCapability;
import eu.asangarin.meaddon.block.CustomRemoteControllerBlockEntity;
import fr.shoqapik.btemobs.block.BteAbstractWorkBlock;
import fr.shoqapik.btemobs.block.ExplorerTableBlock;
import fr.shoqapik.btemobs.entity.BlacksmithEntity;
import fr.shoqapik.btemobs.entity.DruidEntity;
import fr.shoqapik.btemobs.entity.ExplorerEntity;
import fr.shoqapik.btemobs.entity.WarlockEntity;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import fr.shoqapik.btemobs.registry.BteMobsEntities;
import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.common.DimensionUtil;
import mc.duzo.ender_journey.common.block_entity.ColumnBlockEntity;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.data.Savable;
import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
		private BlockPos centre;
		private final int[] posElevator=new int[]{1,-1};
		private final List<BlockPos> posElevators=List.of(new BlockPos(0,112,-38),new BlockPos(0,80,-38),new BlockPos(0,53,-38));
		public Structure(ResourceLocation structure, @Nullable BlockPos centre) {
			this.structure = structure;
			this.isPlaced = false;
			this.centre = centre;
		}
		public Structure() {
			this(getDefaultStructure(), new BlockPos(0, 143, 0)); // default island structure
		}
		public Structure(CompoundTag data) {
			this.structure = new ResourceLocation(data.getString("Structure"));

			this.deserialise(data);
		}

		@Override
		public CompoundTag serialise() {
			CompoundTag data = new CompoundTag();

			data.putString("Structure", this.structure.toString());

			data.putBoolean("isPlaced", this.isPlaced);
			if (this.centre != null)
				data.put("Centre", NbtUtils.writeBlockPos(this.centre));

			return data;
		}

		@Override
		public void deserialise(CompoundTag data) {
			this.isPlaced = data.getBoolean("isPlaced");

			if (data.contains("Centre")) {
				this.centre = NbtUtils.readBlockPos(data.getCompound("Centre"));
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

		/**
		 * places the structure if it is not already placed
		 */
		public void verify() {
			if (!this.isPlaced()) {
				this.place();
			}
		}

		private void place() {
			this.place(getDimension(), true);
		}

		private void place(ServerLevel level, boolean inform) {
			if(level==null)return;
			if (inform && level!=null) {
				for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
					p.sendSystemMessage(Component.literal("Please wait while the structure is placed..."));
				}

			}

			long start = System.currentTimeMillis();


			if (this.isPlaced()) {
				EndersJourney.LOGGER.warn("Tried to place realm structure twice");
			}

			makeInitialIsland(level,start);
			makeInitialRoom(level,start);
			makePortals(level,start);
			makeSuperiorIsland(level,start);

			placeSign(start,level);


			settingElevator(level);

			placeColumn(level);

			if(ModList.get().isLoaded("bte_mobs")){
				this.spawnBlackSmith(level);
				this.spawnDruid(level);
				this.spawnWarlock(level);
				this.spawnExplorer(level);
			}
			this.isPlaced = true;

		}

		public void spawnExplorer(ServerLevel level){
			BlockPos pos = new BlockPos(3,132,-69);
			ExplorerEntity entity = new ExplorerEntity(BteMobsEntities.EXPLORER_ENTITY.get(),level);
			entity.setPos(3.5F,133,-69.5F);
			entity.setTablePos(pos);
			entity.setAttachFace(Direction.SOUTH);
			level.addFreshEntity(entity);
			BlockState table = BteMobsBlocks.EXPLORER_TABLE.get().defaultBlockState().setValue(ExplorerTableBlock.FACING, Direction.SOUTH);
			level.setBlock(pos,table,3);
		}

		public void spawnDruid(ServerLevel level){
			BlockPos pos = new BlockPos(-3,129,54);
			DruidEntity entity = new DruidEntity(BteMobsEntities.DRUID_ENTITY.get(),level);
			entity.setPos(-2.5F,129,53.9F);
			entity.setAttachFace(Direction.NORTH);
			entity.setYRot(Direction.NORTH.toYRot());
			level.addFreshEntity(entity);
			BlockState magmaBlock = BteMobsBlocks.ORIANA_OAK.get().defaultBlockState();
			level.setBlock(pos,magmaBlock,3);
			entity.tablePos = pos;
		}

		public void spawnWarlock(ServerLevel level){
			WarlockEntity entity = new WarlockEntity(BteMobsEntities.WARLOCK_ENTITY.get(),level);
			entity.setPos(-62.5F,134,0.5F);
			entity.setAttachFace(Direction.EAST);
			entity.setYRot(Direction.EAST.toYRot());
			level.addFreshEntity(entity);
		}
		public void spawnBlackSmith(ServerLevel level){
			Entity entity = new BlacksmithEntity(BteMobsEntities.BLACKSMITH_ENTITY.get(),level);
			entity.setPos(62.5F,134,0.5F);
			entity.setYBodyRot(Direction.WEST.toYRot());
			level.addFreshEntity(entity);
			BlockState magmaBlock = BteMobsBlocks.MAGMA_FORGE.get().defaultBlockState().setValue(BteAbstractWorkBlock.FACING,Direction.EAST);
			level.setBlock(new BlockPos(61,133,0),magmaBlock,3);
		}


		private void placeColumn(ServerLevel level) {
			DimensionUtil.eyeItemForBlockPos.forEach(((itemStack, pos) -> {
				ColumnBlockEntity block =new ColumnBlockEntity(pos,BKBlocks.COLUMN.get().defaultBlockState());
				level.setBlock(pos,block.getBlockState(),3);
				ColumnBlockEntity blockEntity = (ColumnBlockEntity) level.getBlockEntity(pos);
				if(blockEntity!=null){
					blockEntity.setItem(DimensionUtil.getItem(itemStack));
				}
			}));
			for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9,50,-10),new BlockPos(9,50,10))){
				level.setBlock(pos, Blocks.END_STONE_BRICKS.defaultBlockState(),3);

			}
			for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9,51,-10),new BlockPos(9,51,10))){
				if(level.isEmptyBlock(pos)){
					level.setBlock(pos, BKBlocks.THE_NEW_END_PORTAL.get().defaultBlockState(),3);
				}
			}
			for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(33,94,-3),new BlockPos(33,86,3))){
				level.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(),3);
			}
			for(BlockPos pos : BlockPos.betweenClosed(new BlockPos(-32,94,3),new BlockPos(-32,85,-4))){
				level.setBlock(pos, Blocks.NETHER_BRICKS.defaultBlockState(),3);

			}
		}

		public void placeSign(long start,ServerLevel level){
			StructurePlaceSettings settings=new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_180);
			this.placeComponent(start,level,11,119,6,new ResourceLocation(EndersJourney.MODID, "cartel_cognition"), settings);
			this.placeComponent(start,level,-11,119,6,new ResourceLocation(EndersJourney.MODID, "cartel_waystone"), settings);
			this.placeComponent(start,level,6,119,11,new ResourceLocation(EndersJourney.MODID, "cartel_ocultismo"), settings);
		}

		public void settingElevator(ServerLevel level){
			ElevatorGroupCapability cap=ElevatorGroupCapability.get(level);
			boolean isFirstConfig=true;
			for (BlockPos pos:this.posElevators){
				ControllerBlockEntity controller= (ControllerBlockEntity) level.getBlockEntity(pos);
				if (controller!=null){
					cap.add(controller);
					for (int i=0;i<2;i++){
						BlockPos relativePos=pos.offset(this.posElevator[i],1,1);
						CustomRemoteControllerBlockEntity remote= (CustomRemoteControllerBlockEntity) level.getBlockEntity(relativePos);
						if(remote!=null){
							remote.setCamoState(Blocks.BLACKSTONE.defaultBlockState());
							remote.setValues(remote.getFacing(),pos,controller.getFacing());
						}
					}
					for (int i=0;i<2;i++){
						BlockPos relativePos=pos.offset(this.posElevator[i]*3,1,isFirstConfig ? 9 : 10);
						CustomRemoteControllerBlockEntity remote= (CustomRemoteControllerBlockEntity) level.getBlockEntity(relativePos);
						if(remote!=null){
							remote.setCamoState(Blocks.BLACKSTONE.defaultBlockState());
							remote.setValues(remote.getFacing(),pos,controller.getFacing());
						}
					}
					if(isFirstConfig){
						ElevatorGroup data=cap.get(0,-38, controller.getFacing());
						if(data!=null){
							data.increaseCageDepthOffset();
							data.setTargetSpeed(0.8F);
							for (int i=0;i<4;i++){
								data.increaseCageDepth();
								data.increaseCageWidth();
							}
							for (int j=0;j<3;j++){
								data.decreaseCageHeight();
							}
						}
						isFirstConfig=false;
                    }
				}
			}
		}

		public void makeSuperiorIsland(ServerLevel level, long start){
			StructurePlaceSettings settings=new StructurePlaceSettings();
			this.placeComponent(start,level,5,111,-4,new ResourceLocation(EndersJourney.MODID, "temple"), settings);
		}

		public void makePortals(ServerLevel level, long start){
			StructurePlaceSettings settings=new StructurePlaceSettings();
			this.placeComponent(start,level,-7,78,3,new ResourceLocation(EndersJourney.MODID, "island_superior"),settings);
		}

		public void placeComponent(long start,ServerLevel level,int addX,int height,int addZ,ResourceLocation location,StructurePlaceSettings settings){
			StructureTemplate component = this.findStructure(location).orElse(null);

			if (component == null) {
				EndersJourney.LOGGER.error("Could not find realm component :" + location.toString());
				return;
			}
			Vec3i size = component.getSize();
			BlockPos offset = new BlockPos(-size.getX() / 2+addX, height, -size.getZ() / 2 +addZ);


			placeInWorld(component,level, offset, offset, settings, level.getRandom(), Block.UPDATE_NONE | Block.UPDATE_SUPPRESS_LIGHT);

			EndersJourney.LOGGER.info("Placed " + this + " at " + offset + " in " + (System.currentTimeMillis() - start) + "ms");
		}

		public boolean placeInWorld(StructureTemplate component, ServerLevelAccessor world, BlockPos templatePos, BlockPos offsetPos, StructurePlaceSettings settings, RandomSource random, int flags) {
			if (component.palettes.isEmpty()) return false;

			List<StructureTemplate.StructureBlockInfo> blocks = settings.getRandomPalette(component.palettes, templatePos).blocks();
			if (blocks.isEmpty()) return false;
			if (component.getSize().getX() < 1 || component.getSize().getY() < 1 || component.getSize().getZ() < 1) return false;

			List<Pair<BlockPos, CompoundTag>> blockEntitiesToLoad = new ArrayList<>(blocks.size());

			for (StructureTemplate.StructureBlockInfo info : processBlockInfos(world, templatePos, offsetPos, settings, blocks, component)) {
				BlockPos pos = info.pos;
				BlockState state = info.state.mirror(settings.getMirror()).rotate(settings.getRotation());

				if (state.isAir()) continue;

				world.setBlock(pos, state, flags & ~2);

				if (info.nbt != null) {
					blockEntitiesToLoad.add(Pair.of(pos, info.nbt));
				}
			}

			for (Pair<BlockPos, CompoundTag> pair : blockEntitiesToLoad) {
				BlockPos pos = pair.getFirst();
				CompoundTag nbt = pair.getSecond();
				BlockEntity be = world.getBlockEntity(pos);
				if (be != null) {
					if (be instanceof RandomizableContainerBlockEntity) {
						nbt.putLong("LootTableSeed", random.nextLong());
					}
					be.load(nbt);
				}
			}

			return true;
		}

		public void makeInitialIsland(ServerLevel level, long start){
			StructurePlaceSettings settings=new StructurePlaceSettings();

			this.placeComponent(start,level,10,-1,0,new ResourceLocation(EndersJourney.MODID, "island_inferior"),settings);
		}

		public void makeInitialRoom(ServerLevel level, long start){
			StructurePlaceSettings settings=new StructurePlaceSettings();
			this.placeComponent(start,level,9,48,-4,new ResourceLocation(EndersJourney.MODID, "island_center"),settings);
		}

		public BlockPos getCentre() {
			this.verify();

			if (this.centre != null) return this.centre;

			StructureTemplate found = this.findStructure(getDefaultStructure()).orElse(null);

			if (found == null) {
				EndersJourney.LOGGER.error("Could not find realm structure template");
				return null;
			}

			this.centre = new BlockPos(0, 143, 0);

			EndersJourney.LOGGER.info("Placed " + this +"ms");
			return this.centre;
		}

		@Override
		public String toString() {
			return "RealmStructure{" +
					"structure=" + structure+
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
			if (player instanceof ServerPlayer) {
				this.runSpawnLogic((ServerPlayer) player);
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
			if (this.parent.getStructure().getCentre() == null) return;

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
			seenData.getAllKeys().forEach(key -> this.seen.put(UUID.fromString(key), new RealmPlayer(seenData.getCompound(key))));
		}
	}

	public static class RealmPlayer implements Savable { // unnecessary now
		private final UUID id;
		private ServerPlayer playerCache;

		public RealmPlayer(UUID id) {
			this.id = id;
		}
		public RealmPlayer(ServerPlayer player) {
			this(player.getUUID());

			this.playerCache = player;
		}
		public RealmPlayer(CompoundTag data) {
			this(data.getUUID("ID"));
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
