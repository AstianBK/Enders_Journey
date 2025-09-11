package mc.duzo.beyondtheend.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import mc.duzo.beyondtheend.client.SkyboxHelper;
import mc.duzo.beyondtheend.realm.RealmManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class SkyboxMixin {
	@Inject(method="renderSky", at = @At("HEAD"), cancellable = true)
	public void enderjourney$renderSky(PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
		ClientLevel world = Minecraft.getInstance().level;
		AbstractClientPlayer player = Minecraft.getInstance().player;

		if(world == null || player == null) return;

		if (RealmManager.isInRealm(player)) {
			SkyboxHelper.renderRealmSky(matrices);
			ci.cancel();
		}
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void enderjourney$renderClouds(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, double pCamX, double pCamY, double pCamZ, CallbackInfo ci) {
		if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null && RealmManager.isInRealm(Minecraft.getInstance().player)) {
			ci.cancel();
		}
	}
}