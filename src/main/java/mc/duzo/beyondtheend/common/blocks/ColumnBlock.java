package mc.duzo.beyondtheend.common.blocks;

import mc.duzo.beyondtheend.common.block_entity.ColumnBlockEntity;
import mc.duzo.beyondtheend.common.register.BKBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ColumnBlock extends BaseEntityBlock {
    protected static final VoxelShape AXIS_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 32.0D, 16.0D);

    public ColumnBlock(Properties p_49795_) {
        super(p_49795_);
    }

    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return AXIS_AABB;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return new ColumnBlockEntity(p_153215_,p_153216_);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_60572_, BlockGetter p_60573_, BlockPos p_60574_, CollisionContext p_60575_) {
        return AXIS_AABB;
    }
}
