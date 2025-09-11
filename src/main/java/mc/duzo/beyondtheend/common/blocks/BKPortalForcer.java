package mc.duzo.beyondtheend.common.blocks;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.common.register.BkPoi;
import mc.duzo.beyondtheend.mixin.common.EntityAccessor;
import mc.duzo.beyondtheend.world.dimension.EnderDimensions;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class BKPortalForcer implements ITeleporter {
    private final ServerLevel level;
    private final boolean hasFrame;
    private final boolean isStartup;

    public BKPortalForcer(ServerLevel level, boolean hasFrame) {
        this.level = level;
        this.hasFrame = hasFrame;
        this.isStartup = false;
    }

    public BKPortalForcer(ServerLevel level, boolean hasFrame, boolean isStartup) {
        this.level = level;
        this.hasFrame = hasFrame;
        this.isStartup = isStartup;
    }

    @Override
    public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceLevel, ServerLevel destinationLevel) {
        if (this.hasFrame) {
            //PacketRelay.sendToPlayer(AetherPacketHandler.INSTANCE, new PortalTravelSoundPacket(), player);
        }
        return false;
    }


    @Nullable
    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destinationLevel, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        EntityAccessor entityAccessor = (EntityAccessor) entity;
        boolean isAether = destinationLevel.dimension() == PortalBlock.destinationDimension();
        if (entity.level.dimension() != PortalBlock.destinationDimension() && !isAether) {
            return null;
        } else {
            WorldBorder worldBorder = destinationLevel.getWorldBorder();
            return this.getExitPortal(entity, new BlockPos(level.random.nextInt(100)*level.random.nextInt(100),60,level.random.nextInt(100)*level.random.nextInt(100)) , worldBorder).map((rectangle) -> {
                BlockState blockState = this.level.getBlockState(entityAccessor.aether$getPortalEntrancePos());
                Direction.Axis axis;
                Vec3 vec3;
                if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                    axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                    BlockUtil.FoundRectangle foundRectangle = BlockUtil.getLargestRectangleAround(entityAccessor.aether$getPortalEntrancePos(), axis, 21, Direction.Axis.Y, 21, (blockPos) -> this.level.getBlockState(blockPos) == blockState);
                    vec3 = entityAccessor.callGetRelativePortalPosition(axis, foundRectangle);
                } else {
                    axis = Direction.Axis.X;
                    vec3 = new Vec3(0, 0.0, 0.0);
                }
                if(destinationLevel.dimension()== EnderDimensions.REALM_KEY){
                    return new PortalInfo(Vec3.atCenterOf(new BlockPos(0,143,0)), Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }else if(destinationLevel.dimension()!=Level.END){
                    return PortalShape.createPortalInfo(destinationLevel, rectangle, axis, vec3,entity.getDimensions(entity.getPose()), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
                }else {
                    return new PortalInfo(Vec3.atCenterOf(ServerLevel.END_SPAWN_POINT), Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }
            }).orElse(null);
        }
    }

    private Optional<BlockUtil.FoundRectangle> getExitPortal(Entity entity, BlockPos findFrom, WorldBorder worldBorder) {
        EntityAccessor entityAccessor = (EntityAccessor) entity;
        if (entity instanceof ServerPlayer) {
            if(entity.level.dimension()!=Level.END){
                Direction.Axis direction$axis = this.level.getBlockState(entityAccessor.aether$getPortalEntrancePos()).getOptionalValue(PortalBlock.AXIS).orElse(Direction.Axis.X);
                Optional<BlockUtil.FoundRectangle> portalOptional = this.createPortal(findFrom, direction$axis);
                if (portalOptional.isEmpty()) {
                    EndersJourney.LOGGER.error("Unable to create an Portal, likely target out of worldborder");
                }
                return portalOptional;
            }else {
                Direction.Axis direction$axis = this.level.getBlockState(entityAccessor.aether$getPortalEntrancePos()).getOptionalValue(PortalBlock.AXIS).orElse(Direction.Axis.X);
                return this.createPortal(ServerLevel.END_SPAWN_POINT,direction$axis);
            }

        } else {
            Direction.Axis direction$axis = this.level.getBlockState(entityAccessor.aether$getPortalEntrancePos()).getOptionalValue(PortalBlock.AXIS).orElse(Direction.Axis.X);
            return this.createPortal(ServerLevel.END_SPAWN_POINT,direction$axis);
        }
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double d0 = -1.0;
        BlockPos blockPos = null;
        double d1 = -1.0;
        BlockPos blockPos1 = null;
        WorldBorder worldBorder = this.level.getWorldBorder();
        int i = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
        BlockPos.MutableBlockPos mutablePos = pos.mutable();

        for (BlockPos.MutableBlockPos mutablePos1 : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH)) {
            int j = Math.min(i, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, mutablePos1.getX(), mutablePos1.getZ()));
            if (worldBorder.isWithinBounds(mutablePos1) && worldBorder.isWithinBounds(mutablePos1.move(direction, 1))) {
                mutablePos1.move(direction.getOpposite(), 1);

                for (int l = j; l >= this.level.getMinBuildHeight(); --l) {
                    mutablePos1.setY(l);
                    if (this.level.isEmptyBlock(mutablePos1)) {
                        int i1;
                        for (i1 = l; l > this.level.getMinBuildHeight() && this.level.isEmptyBlock(mutablePos1.move(Direction.DOWN)); --l) { }

                        if (l + 4 <= i) {
                            int j1 = i1 - l;
                            if (j1 <= 0 || j1 >= 3) {
                                mutablePos1.setY(l);
                                if (this.canHostFrame(mutablePos1, mutablePos, direction, 0)) {
                                    double d2 = pos.distSqr(mutablePos1);
                                    if (this.canHostFrame(mutablePos1, mutablePos, direction, -1) && this.canHostFrame(mutablePos1, mutablePos, direction, 1) && (d0 == -1.0 || d0 > d2)) {
                                        d0 = d2;
                                        blockPos = mutablePos1.immutable();
                                    }
                                    if (d0 == -1.0 && (d1 == -1.0 || d1 > d2)) {
                                        d1 = d2;
                                        blockPos1 = mutablePos1.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if(blockPos==null){
            int k1 = Math.max(this.level.getMinBuildHeight() - -1, 70);
            int i2 = i - 9;
            blockPos = (new BlockPos(pos.getX(), Mth.clamp(pos.getY(), k1, i2), pos.getZ())).immutable();
        }
        i = blockPos.getX();
        int j = blockPos.getY() - 2;
        int k = blockPos.getZ();
        if(!this.level.getBlockState(blockPos).isAir() && !this.level.getBlockState(blockPos.above()).isAir()){
            BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((p_207578_) -> {
                this.level.setBlockAndUpdate(p_207578_, Blocks.AIR.defaultBlockState());
            });
        }

        if(this.level.getBlockState(blockPos.below()).isAir() || !this.level.getBlockState(blockPos.below()).getFluidState().isEmpty()){
            BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((p_184101_) -> {
                this.level.setBlockAndUpdate(p_184101_, Blocks.OBSIDIAN.defaultBlockState());
            });
        }
        return Optional.of(new BlockUtil.FoundRectangle(blockPos.immutable(), 2, 3));
    }

    private boolean canHostFrame(BlockPos originalPos, BlockPos.MutableBlockPos offsetPos, Direction direction, int offsetScale) {
        Direction clockWiseDirection = direction.getClockWise();
        for (int i = -1; i < 3; ++i) {
            for (int j = -1; j < 4; ++j) {
                offsetPos.setWithOffset(originalPos, direction.getStepX() * i + clockWiseDirection.getStepX() * offsetScale, j, direction.getStepZ() * i + clockWiseDirection.getStepZ() * offsetScale);
                BlockState blockState = this.level.getBlockState(offsetPos);
                if (j < 0 && (!blockState.getMaterial().isSolid()
                        || blockState.is(Blocks.END_STONE))) {
                    return false;
                }
                if (j >= 0 && !this.level.isEmptyBlock(offsetPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    private BlockPos checkPositionsForInitialSpawn(Level level, BlockPos origin) {
        if (!this.isSafe(level, origin)) {
            for (int i = 0; i <= 750; i += 5) {
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    BlockPos offsetPosition = origin.offset(facing.getNormal().multiply(i));
                    if (this.isSafeAround(level, offsetPosition)) {
                        return offsetPosition;
                    }
                    BlockPos heightmapPosition = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, offsetPosition);
                    if (this.isSafeAround(level, heightmapPosition)) {
                        return heightmapPosition;
                    }
                }
            }
        }
        return origin;
    }

    public boolean isSafeAround(Level level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        if (!this.isSafe(level, belowPos)) {
            return false;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (!this.isSafe(level, belowPos.relative(facing, 2))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafe(Level level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos) && level.getBlockState(pos).is(Blocks.END_STONE) && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir();
    }

}
