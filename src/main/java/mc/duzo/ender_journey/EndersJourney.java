package mc.duzo.ender_journey;

import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3d;
import mc.duzo.ender_journey.capabilities.PortalPlayerCapability;
import mc.duzo.ender_journey.client.ClientProxy;
import mc.duzo.ender_journey.common.register.BKBlockEntity;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.common.register.BkPoi;
import mc.duzo.ender_journey.data.global.server.ServerData;
import mc.duzo.ender_journey.network.PacketHandler;
import mc.duzo.ender_journey.realm.RealmManager;
import mc.duzo.ender_journey.sound.EnderSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EndersJourney.MODID)
public class EndersJourney {

    public static final String MODID = "ender_journey";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static CommonProxy PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);


    public EndersJourney() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        EnderSounds.register(bus);
        PacketHandler.registerMessages();
        BKBlocks.BLOCKS.register(bus);
        BKBlocks.ITEMS.register(bus);
        BkPoi.POI.register(bus);
        BKBlockEntity.BLOCK_ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);

    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> PROXY.init());
    }


    private void commonSetup(final FMLCommonSetupEvent event) {}

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if(ServerData.get().getRealmManager()==null)return;

        ServerData.get().getRealmManager().getPlayer().onJoin(event.getEntity());
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            if(ServerData.get().getRealmManager()==null)return;

            ServerData.get().getRealmManager().getStructure().verify();
            ServerData.get().getRealmManager().getStructure().tick();
        }
    }
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if(ServerData.get().getRealmManager()==null)return;
        ServerData.get().getRealmManager().getPlayer().onLeave(event.getEntity());
    }
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(ServerData.get().getRealmManager()==null)return;
        ServerData.get().getRealmManager().getPlayer().onRespawn(event.getEntity());
    }
    @SubscribeEvent
    public void onEntitySpawn(LivingSpawnEvent.CheckSpawn event) {
        if (RealmManager.isInRealm(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (!player.level.dimension().location().equals(new ResourceLocation("ender_journey", "the_forgotten_realm")))
            return;

        ChunkPos chunk = new ChunkPos(event.getPos());

        if (!PortalPlayerCapability.isChunkUnlocked(player, chunk.x,chunk.z)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()){
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getPlayer();

        ChunkPos chunk = new ChunkPos(event.getPos());

        if (!PortalPlayerCapability.isChunkUnlocked(player, chunk.x,chunk.z)) {
            event.setCanceled(true);
        }
    }


    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static Vector3d getCentre(BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
