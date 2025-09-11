package mc.duzo.beyondtheend.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import mc.duzo.beyondtheend.EndersJourney;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class SkyboxHelper {
	private static final ResourceLocation REALM_SKY = new ResourceLocation(EndersJourney.MODID, "textures/environment/realm_sky.png");

	public static void renderCustomSky(PoseStack matrices, ResourceLocation texture) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, texture);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();

		for(int i = 0; i < 6; ++i) {
			matrices.pushPose();
			if (i == 1) {
				matrices.mulPose(Vector3f.XP.rotationDegrees(90.0F));
			}

			if (i == 2) {
				matrices.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
			}

			if (i == 3) {
				matrices.mulPose(Vector3f.XP.rotationDegrees(180.0F));
			}

			if (i == 4) {
				matrices.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
			}

			if (i == 5) {
				matrices.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
			}

			Matrix4f matrix4f = matrices.last().pose();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).color(40, 40, 40, 255).endVertex();
			tesselator.end();
			matrices.popPose();
		}
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	public static void renderRealmSky(PoseStack matrices) {
		renderCustomSky(matrices, REALM_SKY);
	}
}
