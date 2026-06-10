package mc.duzo.ender_journey.capabilities;

import mc.duzo.ender_journey.EndersJourney;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public class BkCapabilities {
    public static final Capability<PortalPlayer> PORTAL_PLAYER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });

    public static final Capability<IZoneChunkCapability> CHUNK_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PortalPlayer.class);
        event.register(IZoneChunkCapability.class);
    }
    @SuppressWarnings("unchecked")
    public static <T extends IZoneChunkCapability> T getWorldCapability(Level level, Class<T> type) {
        if (level != null) {
            IZoneChunkCapability entitypatch = level.getCapability(BkCapabilities.CHUNK_CAPABILITY).orElse(null);

            if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
                return (T)entitypatch;
            }
        }

        return null;
    }
    @SuppressWarnings("unchecked")
    public static <T extends PortalPlayer> T getEntityPatch(Entity entity, Class<T> type) {
        if (entity != null) {
            PortalPlayer entitypatch = entity.getCapability(BkCapabilities.PORTAL_PLAYER_CAPABILITY).orElse(null);

            if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
                return (T)entitypatch;
            }
        }

        return null;
    }
}
