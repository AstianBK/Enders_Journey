package mc.duzo.ender_journey.mixin.common;

import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels application of protectyourstructures:sacred_place at the source (before any
 * network sync packet is sent to the client) when the target is a ServerPlayer standing
 * in the_forgotten_realm but outside the central island's bounding box. This prevents the
 * add-then-immediately-remove visual flicker that happens when reacting to the effect
 * after the fact (e.g. via a tick handler), since by then the client has already been
 * told the effect was added.
 */
@Mixin(LivingEntity.class)
public class SacredPlaceEffectMixin {

    // "Central island" = within 1000 blocks (horizontal, X/Z) of the world origin.
    // Must match Events.isWithinCentralIsland exactly.
    private static final double CENTRAL_ISLAND_RADIUS_SQ = 1000.0 * 1000.0;

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void enderjourney$blockSacredPlaceOutsideIsland(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect());
        if (effectId == null || !effectId.getNamespace().equals("protectyourstructures") || !effectId.getPath().equals("sacred_place")) {
            return;
        }

        if (!player.getLevel().dimension().equals(EnderDimensions.REALM_KEY)) return;

        BlockPos pos = player.blockPosition();
        double distSq = (double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ();
        boolean withinCentralIsland = distSq <= CENTRAL_ISLAND_RADIUS_SQ;

        if (!withinCentralIsland) {
            cir.setReturnValue(false);
        }
    }
}
