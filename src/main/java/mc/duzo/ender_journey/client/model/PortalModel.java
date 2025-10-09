package mc.duzo.ender_journey.client.model;

import com.google.common.collect.ImmutableList;
import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import mc.duzo.ender_journey.common.register.BKBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortalModel<T extends BakedModel> extends BakedModelWrapper<T> {

    public PortalModel(T originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if(state!=null && cantRenderer(state)){
            return ImmutableList.of();
        }
        return super.getQuads(state, side, rand);
    }

    public boolean cantRenderer(BlockState pState){
        EndersJourney.LOGGER.debug("entro al super mixin");
        if(pState.getRenderShape()== RenderShape.MODEL){
            if(Minecraft.getInstance().player!=null){
                PortalPlayer player= PortalPlayer.get(Minecraft.getInstance().player).orElse(null);
                if(player!=null){
                    if(player.getEyesEarn()<8){
                        return pState.is(BKBlocks.PORTAL_NETHER.get()) || pState.is(BKBlocks.PORTAL.get());
                    }
                    if(player.getEyesEarn()<16){
                        return pState.is(BKBlocks.PORTAL.get());
                    }
                }
            }
        }
        return false;
    }
}
