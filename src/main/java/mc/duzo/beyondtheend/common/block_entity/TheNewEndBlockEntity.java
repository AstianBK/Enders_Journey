package mc.duzo.beyondtheend.common.block_entity;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import mc.duzo.beyondtheend.common.register.BKBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TheNewEndBlockEntity extends TheEndPortalBlockEntity {
    protected TheNewEndBlockEntity(BlockEntityType<?> p_155855_, BlockPos p_155856_, BlockState p_155857_) {
        super(p_155855_, p_155856_, p_155857_);
    }

    public TheNewEndBlockEntity(BlockPos p_155859_, BlockState p_155860_) {
        this(BKBlockEntity.THE_NEW_END_PORTAL_ENTITY.get(), p_155859_, p_155860_);
    }

    public boolean shouldRenderFace(Direction p_59980_) {
        if(Minecraft.getInstance().player==null){
            return false;
        }
        boolean flag=PortalPlayer.get(Minecraft.getInstance().player).orElse(null).getEyesEarn()>=24;
        return p_59980_.getAxis() == Direction.Axis.Y && flag;
    }
}
