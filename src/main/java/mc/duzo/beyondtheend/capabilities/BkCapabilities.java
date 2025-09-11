package mc.duzo.beyondtheend.capabilities;

import mc.duzo.beyondtheend.EndersJourney;
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

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PortalPlayer.class);
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

    @Mod.EventBusSubscriber(modid = EndersJourney.MODID)
    public static class Registration {

        @SubscribeEvent
        public static void attachWorldCapabilities(AttachCapabilitiesEvent<Level> event) {

        }
    }
}
