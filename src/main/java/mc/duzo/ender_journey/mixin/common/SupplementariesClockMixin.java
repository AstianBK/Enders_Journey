package mc.duzo.ender_journey.mixin.common;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplementaries' clock block only shows the time in "natural" dimensions (the Overworld)
 * and spins randomly everywhere else. Every vanilla/datapack dimension shares the Overworld's
 * level data, so Level#getDayTime() already returns the Overworld time there: we just let the
 * clock read it in every dimension (the_forgotten_realm, Twilight Forest, Nether, End...).
 */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.ClockBlockTile", remap = false)
public class SupplementariesClockMixin {

    @Inject(method = "canReadTime", at = @At("HEAD"), cancellable = true, remap = false)
    private static void enderjourney$alwaysReadOverworldTime(Level level, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
