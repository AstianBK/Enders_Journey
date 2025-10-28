package mc.duzo.ender_journey;

import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3d;
import mc.duzo.ender_journey.client.ClientProxy;
import mc.duzo.ender_journey.common.register.BKBlockEntity;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.common.register.BkPoi;
import mc.duzo.ender_journey.data.global.server.ServerData;
import mc.duzo.ender_journey.network.PacketHandler;
import mc.duzo.ender_journey.realm.RealmManager;
import mc.duzo.ender_journey.sound.EnderSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
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

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static Vector3d getCentre(BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
