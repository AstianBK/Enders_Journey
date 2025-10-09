package mc.duzo.ender_journey.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import mc.duzo.ender_journey.common.DimensionUtil;
import mc.duzo.ender_journey.common.block_entity.ColumnBlockEntity;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class ColumnRenderer <T extends ColumnBlockEntity> implements BlockEntityRenderer<T> {
    public final ItemRenderer renderer;
    public ColumnRenderer(BlockEntityRendererProvider.Context context){
        this.renderer=context.getItemRenderer();
    }


    @Override
    public void render(T p_112307_, float p_112308_, PoseStack p_112309_, MultiBufferSource p_112310_, int p_112311_, int p_112312_) {
        assert Minecraft.getInstance().player!=null;
        PortalPlayer.get(Minecraft.getInstance().player).ifPresent(portalPlayer->{
            long time = p_112307_.getLevel().getGameTime();
            ResourceLocation location=DimensionUtil.build(ForgeRegistries.ITEMS.getKey(p_112307_.getItem().getItem()).toString().split(":")[1]);
            if(!p_112307_.getItem().isEmpty() && portalPlayer.haveEye(location) ){
                p_112309_.pushPose();
                p_112309_.translate(0.5F,2.5F,0.5F);
                p_112309_.scale(0.5F,0.5F,0.5F);
                int scale = (int)((Double) ClientConfigs.Blocks.PEDESTAL_SPEED.get() * 360.0);
                float angle = ((float)Math.floorMod(time, (long)scale) + Minecraft.getInstance().getPartialTick()) / (float)scale;
                Quaternion rotation = Vector3f.YP.rotation((float)((double)angle * Math.PI * 10.0));
                p_112309_.mulPose(rotation);

                this.renderer.renderStatic(p_112307_.getItem(), ItemTransforms.TransformType.FIXED,p_112311_, OverlayTexture.NO_OVERLAY,p_112309_,p_112310_,0);
                p_112309_.popPose();
            }
        });
    }
}
