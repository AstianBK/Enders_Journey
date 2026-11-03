package mc.duzo.ender_journey.realm;

import com.mojang.math.Vector3d;
import com.supermartijn642.movingelevators.blocks.ControllerBlockEntity;
import com.supermartijn642.movingelevators.elevator.ElevatorGroup;
import com.supermartijn642.movingelevators.elevator.ElevatorGroupCapability;
import eu.asangarin.meaddon.block.CustomRemoteControllerBlockEntity;
import fr.shoqapik.btemobs.block.BteAbstractWorkBlock;
import fr.shoqapik.btemobs.block.ExplorerTableBlock;
import fr.shoqapik.btemobs.entity.*;
import fr.shoqapik.btemobs.registry.BteMobsBlocks;
import fr.shoqapik.btemobs.registry.BteMobsEntities;
import mc.duzo.ender_journey.CommonProxy;
import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.common.DimensionUtil;
import mc.duzo.ender_journey.common.block_entity.ColumnBlockEntity;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.data.Savable;
import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import tictim.paraglider.contents.Contents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import mc.duzo.ender_journey.common.blocks.BKPortalForcer;
import java.util.*;

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
		// Primer spawn en el Realm: mirando al Este (yaw -90), hacia Anna Warhammer
		EnderDimensions.teleport(entity, getDimension(), vec, -90.0F, 0.0F);
	}

	public static class Structure implements Savable {

		private final ResourceLocation structure;
		private boolean isPlaced;
		private boolean plataformPlaced;
		private boolean sessionColumnGenerated = false;
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
		/**
		 * Tareas minimas por llamada a tick() (lo que se hacia siempre). tick() se llama una vez por cada
		 * dimension cargada en cada tick del servidor, asi que nunca va mas lento que antes.
		 */
		private static final int TASKS_PER_TICK = 2;
		/**
		 * Ademas de las minimas, se siguen ejecutando tareas mientras el trabajo de este tick del servidor no
		 * pase de este tiempo. Con jugadores conectados se deja margen para que el juego siga fluido; sin
		 * nadie conectado (creando el mundo) nadie nota los tirones. Asi cada PC coloca lo que puede: uno
		 * potente termina mucho antes y uno flojo no se congela mas que antes. El orden de las tareas no
		 * cambia, asi que el Realm queda exactamente igual.
		 */
		private static final long BUDGET_WITH_PLAYERS_NS = 40_000_000L;
		private static final long BUDGET_NO_PLAYERS_NS = 250_000_000L;
		/** Tick del servidor al que corresponde budgetUsedNs (tick() se llama varias veces por tick). */
		private int budgetTick = Integer.MIN_VALUE;
		private long budgetUsedNs;

		// --- Medicion de la generacion (solo para el log) ---
		private long genStartNs;
		private int genStartServerTick;
		private int workTicks;
		private long workNs;
		private long maxTickWorkNs;
		private int blocksQueued;
		private int blocksPlaced;
		private int pendingBlockBatches;
		private long blocksDoneNs;
		private long npcsDoneNs;

		// --- Estado de sesion (NO se guarda en el .dat) ---
		// isPlaced solo pasa a true cuando la estructura esta colocada Y sus chunks
		// estan escritos en disco. Antes se marcaba al empezar a colocar, y el .dat
		// (que se guarda en cada pausa) podia llegar al disco sin los chunks: tras un
		// crash el Realm quedaba vacio para siempre.
		private boolean integrityChecked = false;
		private boolean npcsDone = false;
		private final Set<Long> touchedChunks = new HashSet<>();
		private static final int INTEGRITY_SAMPLES = 24;
		private static final TicketType<ChunkPos> REALM_TICKET =
				TicketType.create("ender_journey_realm", Comparator.comparingLong(ChunkPos::toLong));
		// Posiciones de los NPC (mismas que en spawnX); sus chunks se mantienen cargados
		// mientras se colocan para poder comprobar si el NPC ya existe.
		private static final List<BlockPos> NPC_POSITIONS = List.of(
				new BlockPos(62, 134, 0),   // Blacksmith
				new BlockPos(-3, 129, 54),  // Druid
				new BlockPos(-62, 134, 0),  // Warlock
				new BlockPos(3, 132, -69),  // Explorer
				new BlockPos(63, 131, -18), // Target dummies
				new BlockPos(61, 131, -18)
		);

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
				touch(finalPos);
				placed.add(new StructureTemplate.StructureBlockInfo(finalPos, info.state, info.nbt));
			}

			int batchSize = 1500;
			blocksQueued += placed.size();

			for (int i = 0; i < placed.size(); i += batchSize) {
				int start = i;
				pendingBlockBatches++;

				tasks.add(() -> {
					int end = Math.min(start + batchSize, placed.size());
					for (int j = start; j < end; j++) {
						StructureTemplate.StructureBlockInfo info = placed.get(j);


						level.setBlock(info.pos, info.state, 3);

						if (info.nbt != null) {
							BlockEntity be = level.getBlockEntity(info.pos);
							if (be != null) {
								be.load(info.nbt);
							}
						}
					}
					blocksPlaced += end - start;
					if (--pendingBlockBatches == 0) blocksDoneNs = System.nanoTime();
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
			CommonProxy.initBlocks();

			if (!sessionColumnGenerated) {
				ServerLevel level = getDimension();
				if (level != null) {
					sessionColumnGenerated = true;
					try {
						placeColumn(level);
						checkAndGeneratePlatform(level);
					} catch (Throwable t) {
						EndersJourney.LOGGER.error("[EJ] Exception in placeColumn/checkAndGeneratePlatform", t);
					}
				}
			}

			if (generating) return;

			if (!this.isPlaced) {
				this.place();
				return;
			}

			if (!integrityChecked) {
				this.checkIntegrity();
			}
		}

		private void place() {
			this.place(getDimension(), true);
		}

		private void place(ServerLevel level, boolean inform) {
			if (level == null ) return;

			// OJO: isPlaced ya NO se pone a true aqui. Se pone en finishGeneration(),
			// cuando todos los bloques estan colocados y sus chunks escritos en disco.
			this.generating = true;
			this.npcsDone = false;
			this.integrityChecked = true;
			this.touchedChunks.clear();

			this.genStartNs = System.nanoTime();
			this.genStartServerTick = level.getServer().getTickCount();
			this.workTicks = 0;
			this.workNs = 0;
			this.maxTickWorkNs = 0;
			this.blocksQueued = 0;
			this.blocksPlaced = 0;
			this.pendingBlockBatches = 0;
			this.blocksDoneNs = 0;
			this.npcsDoneNs = 0;

			long start = System.currentTimeMillis();
			EndersJourney.LOGGER.info("[EJ] Placing realm structure...");

			enqueueGeneration(level, start);
		}

		public void tick() {
			if (tasks.isEmpty()) return;

			MinecraftServer server = EndersJourney.getServer();
			int serverTick = server != null ? server.getTickCount() : 0;
			if (serverTick != budgetTick) {
				budgetTick = serverTick;
				budgetUsedNs = 0;
				if (generating) workTicks++;
			}
			long budget = server != null && server.getPlayerCount() > 0 ? BUDGET_WITH_PLAYERS_NS : BUDGET_NO_PLAYERS_NS;

			// Solo las tareas que ya estaban en la cola: las que se vuelven a encolar (esperar a los NPC,
			// terminar) se ejecutan en la siguiente llamada, como antes.
			int limit = tasks.size();
			long t0 = System.nanoTime();
			for (int i = 0; i < limit; i++) {
				if (i >= TASKS_PER_TICK && budgetUsedNs + (System.nanoTime() - t0) >= budget) break;
				Runnable task = tasks.poll();
				if (task == null) break;
				task.run();
			}
			long spent = System.nanoTime() - t0;
			budgetUsedNs += spent;
			if (generating) {
				workNs += spent;
				maxTickWorkNs = Math.max(maxTickWorkNs, budgetUsedNs);
			}
		}

		private static void ensureChunkLoaded(ServerLevel level, BlockPos pos) {
			level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
		}


		private void placeColumn(ServerLevel level) {
			DimensionUtil.eyeItemForBlockPos.forEach((itemStack, pos) -> {
				ensureChunkLoaded(level, pos);
				touch(pos);
				ColumnBlockEntity block = new ColumnBlockEntity(pos, BKBlocks.COLUMN.get().defaultBlockState());
				level.setBlock(pos, block.getBlockState(), 3);

				ColumnBlockEntity blockEntity = (ColumnBlockEntity) level.getBlockEntity(pos);
				if (blockEntity != null) {
					blockEntity.setItem(DimensionUtil.getItem(itemStack));
				}
			});

			ensureChunkLoaded(level, new BlockPos(0, 50, 0));
			touchArea(-9, -10, 9, 10);
			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9, 50, -10), new BlockPos(9, 50, 10))) {
				level.setBlock(pos, Blocks.END_STONE_BRICKS.defaultBlockState(), 3);
			}

			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-9, 51, -10), new BlockPos(9, 51, 10))) {
				if (level.isEmptyBlock(pos)) {
					level.setBlock(pos, BKBlocks.THE_NEW_END_PORTAL.get().defaultBlockState(), 3);
				}
			}

			ensureChunkLoaded(level, new BlockPos(0, 89, 32));
			touchArea(-2, 32, 2, 32);
			touchArea(33, -3, 33, 3);
			touchArea(-32, -4, -32, 3);
			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-2, 87, 32), new BlockPos(2, 92, 32))) {
				if (level.isEmptyBlock(pos)) {
					level.setBlock(pos, BKBlocks.PORTAL_OVERWORLD.get().defaultBlockState(), 3);
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
			makeSpawn(level, start);
			makeInitialIsland(level, start);
//			makeInitialRoom(level, start);
//			makePortals(level, start);


//			tasks.add(()->settingElevator(level));
			placeColumn(level);

			checkAndGeneratePlatform(level);

			// Mantener cargados los chunks de los NPC hasta terminar, para que sus
			// entidades guardadas esten cargadas y no se dupliquen al reconstruir.
			addNpcTickets(level);
			final long npcDeadline = level.getGameTime() + 400L; // ~20 s como maximo
			tasks.add(new Runnable() {
				@Override
				public void run() {
					if (!npcEntitiesLoaded(level) && level.getGameTime() < npcDeadline) {
						tasks.add(this); // aun no han cargado las entidades: reintentar
						return;
					}
					if (ModList.get().isLoaded("bte_mobs")) {
						// El NPC5 ya no aparece en la sala de portales
						// Solo se crea cada NPC si no existe ya (reconstruccion tras un crash)
						if (!hasEntityNear(level, BteMobsEntities.BLACKSMITH_ENTITY.get(), 62.5, 134, 0.5))
							spawnBlackSmith(level);
						if (!hasEntityNear(level, BteMobsEntities.DRUID_ENTITY.get(), -2.5, 129, 53.5))
							spawnDruid(level);
						if (!hasEntityNear(level, BteMobsEntities.WARLOCK_ENTITY.get(), -62.5, 134, 0.5))
							spawnWarlock(level);
						if (!hasEntityNear(level, BteMobsEntities.EXPLORER_ENTITY.get(), 3.5, 133, -69.5))
							spawnExplorer(level);
					}
					spawnTargetDummies(level); // ya comprueba uno a uno si existen
					npcsDone = true;
					npcsDoneNs = System.nanoTime();
				}
			});
			placeSign(start, level);

			// Ultima tarea de la cola: escribir los chunks a disco y SOLO entonces
			// marcar isPlaced. Espera a que la tarea de NPCs haya terminado.
			tasks.add(new Runnable() {
				@Override
				public void run() {
					if (!npcsDone) {
						tasks.add(this);
						return;
					}
					finishGeneration(level, start);
				}
			});
		}

		private void finishGeneration(ServerLevel level, long start) {
			int saved = -1;
			long persistStart = System.nanoTime();
			try {
				saved = persistTouchedChunks(level);
			} catch (Throwable t) {
				EndersJourney.LOGGER.error("[EJ] Could not force-save the realm chunks", t);
			}
			long persistNs = System.nanoTime() - persistStart;
			logGenerationStats(level, persistNs);
			removeNpcTickets(level);

			this.isPlaced = true;
			this.generating = false;

			EndersJourney.LOGGER.info("[EJ] Realm structure placed in " + (System.currentTimeMillis() - start)
					+ "ms, " + saved + "/" + touchedChunks.size() + " chunks written to disk");
			touchedChunks.clear();
		}

		/**
		 * Desglose de la generacion para saber donde se va el tiempo: colocar bloques, esperar a los NPC,
		 * guardar los chunks, y cuanto del tick del servidor ha sido trabajo de la generacion.
		 */
		private void logGenerationStats(ServerLevel level, long persistNs) {
			long now = System.nanoTime();
			int serverTicks = level.getServer().getTickCount() - genStartServerTick;
			long wallMs = (now - genStartNs) / 1_000_000L;
			EndersJourney.LOGGER.info(String.format(java.util.Locale.ROOT,
					"[EJ] Realm generation: %d/%d blocks placed after %d ms, NPCs ready after %d ms, chunk save %d ms, total %d ms",
					blocksPlaced, blocksQueued,
					blocksDoneNs > 0 ? (blocksDoneNs - genStartNs) / 1_000_000L : -1L,
					npcsDoneNs > 0 ? (npcsDoneNs - genStartNs) / 1_000_000L : -1L,
					persistNs / 1_000_000L, wallMs));
			EndersJourney.LOGGER.info(String.format(java.util.Locale.ROOT,
					"[EJ] Realm generation: %d server ticks (%.1f ms per tick on average), %d with work; realm work %d ms (%.1f ms per tick on average, max %.1f ms); players online at the end: %d",
					serverTicks, serverTicks > 0 ? (double) wallMs / serverTicks : 0.0D, workTicks,
					workNs / 1_000_000L, workTicks > 0 ? workNs / 1_000_000.0D / workTicks : 0.0D,
					maxTickWorkNs / 1_000_000.0D, level.getServer().getPlayerCount()));
		}

		/**
		 * Escribe a disco, de forma sincrona, todos los chunks que ha tocado la
		 * generacion. Asi, cuando isPlaced=true llegue al .dat, los bloques ya
		 * estan en los .mca aunque el juego crashee justo despues.
		 * Los chunks que ya se han descargado ya estan en la cola de escritura;
		 * flushWorker() espera tambien por ellos.
		 */
		private int persistTouchedChunks(ServerLevel level) {
			ChunkMap chunkMap = level.getChunkSource().chunkMap;
			int saved = 0;
			for (long key : touchedChunks) {
				ChunkPos cp = new ChunkPos(key);
				ChunkAccess chunk = level.getChunkSource().getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				chunkMap.write(cp, ChunkSerializer.write(level, chunk));
				saved++;
			}
			chunkMap.flushWorker();
			return saved;
		}

		/**
		 * Se ejecuta una vez por sesion si isPlaced=true. Comprueba unos cuantos
		 * bloques de "spawn" e "island" en el mundo; si casi ninguno esta, el
		 * Realm se perdio (crash antes de guardar los chunks) y se reconstruye.
		 */
		private void checkIntegrity() {
			ServerLevel level = getDimension();
			if (level == null) return;
			this.integrityChecked = true;

			int[] result = new int[2]; // [0] encontrados, [1] comprobados
			try {
				sampleComponent(level, 0, 130, 0, new ResourceLocation(EndersJourney.MODID, "spawn"), new StructurePlaceSettings(), result);
				sampleComponent(level, 4, -1, -8, new ResourceLocation(EndersJourney.MODID, "island"), new StructurePlaceSettings(), result);
			} catch (Throwable t) {
				EndersJourney.LOGGER.error("[EJ] Realm integrity check failed", t);
				return;
			}
			if (result[1] == 0) return;

			if (result[0] * 4 < result[1]) {
				EndersJourney.LOGGER.warn("[EJ] Realm is marked as placed but only " + result[0] + "/" + result[1]
						+ " sample blocks exist (world not saved before a crash?). Rebuilding the realm.");
				this.place(level, false);
			} else {
				EndersJourney.LOGGER.info("[EJ] Realm integrity OK (" + result[0] + "/" + result[1] + " sample blocks)");
			}
		}

		private void sampleComponent(ServerLevel level, int addX, int height, int addZ, ResourceLocation location, StructurePlaceSettings settings, int[] result) {
			StructureTemplate template = this.findStructure(location).orElse(null);
			if (template == null || template.palettes.isEmpty()) return;

			List<StructureTemplate.StructureBlockInfo> blocks = settings.getRandomPalette(template.palettes, BlockPos.ZERO).blocks();
			Vec3i size = template.getSize();
			BlockPos origin = new BlockPos(-size.getX() / 2 + addX, height, -size.getZ() / 2 + addZ);

			List<StructureTemplate.StructureBlockInfo> solid = new ArrayList<>();
			for (StructureTemplate.StructureBlockInfo info : blocks) {
				if (info.state.isAir() || info.state.is(Blocks.STRUCTURE_VOID) || info.nbt != null) continue;
				solid.add(info);
			}
			if (solid.isEmpty()) return;

			int samples = Math.min(INTEGRITY_SAMPLES, solid.size());
			for (int i = 0; i < samples; i++) {
				StructureTemplate.StructureBlockInfo info = solid.get((int) ((long) i * solid.size() / samples));
				BlockPos pos = origin.offset(info.pos);
				ensureChunkLoaded(level, pos);
				result[1]++;
				if (level.getBlockState(pos).is(info.state.getBlock())) result[0]++;
			}
		}

		private void touch(BlockPos pos) {
			touchedChunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
		}

		private void touchArea(int minX, int minZ, int maxX, int maxZ) {
			for (int cx = minX >> 4; cx <= maxX >> 4; cx++)
				for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++)
					touchedChunks.add(ChunkPos.asLong(cx, cz));
		}

		private void addNpcTickets(ServerLevel level) {
			for (BlockPos p : NPC_POSITIONS) {
				ChunkPos cp = new ChunkPos(p);
				touch(p);
				level.getChunkSource().addRegionTicket(REALM_TICKET, cp, 2, cp);
			}
		}

		private void removeNpcTickets(ServerLevel level) {
			for (BlockPos p : NPC_POSITIONS) {
				ChunkPos cp = new ChunkPos(p);
				level.getChunkSource().removeRegionTicket(REALM_TICKET, cp, 2, cp);
			}
		}

		private static boolean npcEntitiesLoaded(ServerLevel level) {
			for (BlockPos p : NPC_POSITIONS) {
				if (!level.areEntitiesLoaded(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4))) return false;
			}
			return true;
		}

		private static <T extends Entity> boolean hasEntityNear(ServerLevel level, EntityType<T> type, double x, double y, double z) {
			return hasEntityNear(level, type, x, y, z, 8.0D);
		}

		private static <T extends Entity> boolean hasEntityNear(ServerLevel level, EntityType<T> type, double x, double y, double z, double r) {
			AABB box = new AABB(x - r, y - r, z - r, x + r, y + r, z + r);
			return !level.getEntities(type, box, e -> true).isEmpty();
		}


		public void checkAndGeneratePlatform(ServerLevel level) {
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

					ensureChunkLoaded(level, placed.pos());
					touch(placed.pos());
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
			entity.setPos(0.5F, 80, -26.5F);
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

		/** Target Dummies (MmmMmmMmmMmm) del campo de entrenamiento, junto a la forja de Anna, mirando al sur. */
		private static final ResourceLocation TARGET_DUMMY = new ResourceLocation("dummmmmmy", "target_dummy");
		private static final List<BlockPos> TARGET_DUMMY_POSITIONS = List.of(
				new BlockPos(63, 131, -18),
				new BlockPos(61, 131, -18)
		);

		public void spawnTargetDummies(ServerLevel level) {
			// ENTITY_TYPES devuelve el cerdo si el id no existe, así que se comprueba antes
			if (!ModList.get().isLoaded("dummmmmmy") || !ForgeRegistries.ENTITY_TYPES.containsKey(TARGET_DUMMY)) return;
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(TARGET_DUMMY);
			if (type == null) return;

			float yaw = Direction.SOUTH.toYRot();
			for (BlockPos pos : TARGET_DUMMY_POSITIONS) {
				ensureChunkLoaded(level, pos);
				// No duplicar si ya existe (reconstruccion tras un crash)
				if (hasEntityNear(level, type, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D)) continue;
				Entity dummy = type.create(level);
				if (dummy == null) continue;
				dummy.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
				dummy.setYHeadRot(yaw);
				dummy.setYBodyRot(yaw);
				level.addFreshEntity(dummy);
			}
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
		public void makeSpawn(ServerLevel level, long start) {
			placeComponent( level, 0, 130, 0, new ResourceLocation(EndersJourney.MODID, "spawn"), new StructurePlaceSettings());
		}

		public void makeInitialIsland(ServerLevel level, long start) {
			placeComponent( level, 4, -1, -8, new ResourceLocation(EndersJourney.MODID, "island"), new StructurePlaceSettings());
		}

		public void makeInitialRoom(ServerLevel level, long start) {
			placeComponent( level, -4, 48, -4, new ResourceLocation(EndersJourney.MODID, "island_center"), new StructurePlaceSettings());
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
		private static void setScoreboardValue(net.minecraft.world.scores.Scoreboard sb, String name, int value) {
			net.minecraft.world.scores.Objective obj = sb.getObjective(name);
			if (obj == null) {
				obj = sb.addObjective(name, net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
						net.minecraft.network.chat.Component.literal(name),
						net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
			}
			sb.getOrCreatePlayerScore("WORLD", obj).setScore(value);
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

			// Player is still in the Overworld at this point — capture their spawn
			// position before teleporting to the Realm. The player will never see
			// the Overworld — the teleport happens on the same tick as login.
			MinecraftServer server = player.getServer();
			if (server != null && player.level.dimension() == Level.OVERWORLD) {
				net.minecraft.world.scores.Scoreboard scoreboard = server.getScoreboard();
				boolean alreadyCalculated = scoreboard.getObjective("ej_spawn_x") != null;
				if (!alreadyCalculated) {
					BlockPos overworldSpawn = player.blockPosition();
					EndersJourney.LOGGER.info("[EJ] Overworld spawn captured at: " + overworldSpawn);

					net.minecraft.world.scores.Scoreboard sb = server.getScoreboard();

					// Overworld spawn — write directly to scoreboard API (no chat output)
					setScoreboardValue(sb, "ej_spawn_x", overworldSpawn.getX());
					setScoreboardValue(sb, "ej_spawn_y", overworldSpawn.getY());
					setScoreboardValue(sb, "ej_spawn_z", overworldSpawn.getZ());

					// Nether spawn — use findSafeNetherCave, same method as BKPortalForcer
					ServerLevel netherLevel = server.getLevel(Level.NETHER);
					if (netherLevel != null) {
						BlockPos netherSpawn = BKPortalForcer.findSafeNetherCave(netherLevel, netherLevel.random);
						EndersJourney.LOGGER.info("[EJ] Nether spawn found at: " + netherSpawn);
						setScoreboardValue(sb, "ej_nether_x", netherSpawn.getX());
						setScoreboardValue(sb, "ej_nether_y", netherSpawn.getY());
						setScoreboardValue(sb, "ej_nether_z", netherSpawn.getZ());
					}
				}
			}

			this.parent.teleport(player);
			// First spawn: set respawn at centre (0, 142, 0) so the player lands correctly.
			player.setRespawnPosition(RealmManager.getDimension().dimension(), this.parent.getStructure().getCentre(), 0, true, false);

			// Then immediately update respawn to the permanent spawnpoint (0, 116, 0) facing West.
			// West = yRot 90.0f in Minecraft coordinate system.
			player.setRespawnPosition(RealmManager.getDimension().dimension(), new BlockPos(0, 116, 0), 90.0f, true, false);

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
