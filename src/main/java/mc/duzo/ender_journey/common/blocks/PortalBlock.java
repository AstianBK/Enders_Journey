package mc.duzo.ender_journey.common.blocks;

import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.capabilities.BkCapabilities;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import mc.duzo.ender_journey.mixin.common.EntityAccessor;
import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class PortalBlock extends NetherPortalBlock {
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;


    public PortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(ENABLED,false));

    }

    @SuppressWarnings("deprecation")
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        EntityAccessor entityAccessor = (EntityAccessor) entity;
        if (entity instanceof Player &&!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            if (entity.isOnPortalCooldown()) {
                entity.setPortalCooldown();
            } else {
                if (!entity.level.isClientSide() && !pos.equals(entityAccessor.aether$getPortalEntrancePos())) {
                    entityAccessor.aether$setPortalEntrancePos(pos.immutable());
                }
                LazyOptional<PortalPlayer> portalPlayer = entity.getCapability(BkCapabilities.PORTAL_PLAYER_CAPABILITY);
                if (!portalPlayer.isPresent()) {
                    this.handleTeleportation(entity);
                } else {
                    portalPlayer.ifPresent(handler -> {
                        if(handler.getEyesEarn()>=10){
                            handler.setInPortal(true);
                            this.handleTeleportation(entity);
                            handler.setPortalTimer(0);
                        }
                    });
                }
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        return pState;
    }

    private void handleTeleportation(Entity entity) {
        MinecraftServer server = entity.level.getServer();
        ResourceKey<Level> destinationKey = entity.level.dimension() == destinationDimension() ? returnDimension() : destinationDimension();
        if (server != null) {
            ServerLevel destinationLevel = server.getLevel(destinationKey);
            if (destinationLevel != null && !entity.isPassenger()) {
                entity.level.getProfiler().push("portal");
                BlockPos platform = new BlockPos(100, 80, 0);

                destinationLevel.getChunk(platform);

                createEndPlatform(destinationLevel, platform);

                Vec3 tp = Vec3.atCenterOf(platform.above());

                ((ServerPlayer)entity).teleportTo(destinationLevel, tp.x, tp.y, tp.z, entity.getYRot(), entity.getXRot());                entity.level.getProfiler().pop();
            }
        }
    }
    public static void createEndPlatform(ServerLevel level, BlockPos center) {

        BlockPos.betweenClosed(center.offset(-2, 1, -2), center.offset(2, 3, 2)).forEach(pos -> {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        });

        BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 0, 2)).forEach(pos -> {
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        });
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState pState=this.defaultBlockState();
        return switch (pContext.getHorizontalDirection().getAxis()) {
            case Z -> pState.setValue(AXIS, Direction.Axis.X);
            case X -> pState.setValue(AXIS, Direction.Axis.Z);
            default -> pState;
        };
    }


    @Override
    public RenderShape getRenderShape(BlockState p_60550_) {
        return p_60550_.getValue(ENABLED) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ENABLED);
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean isActive=state.getValue(ENABLED);
        if(isActive){
            if (random.nextInt(100) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.1F, false);
            }
            for (int i = 0; i < 4; ++i) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + random.nextDouble();
                double xSpeed = (random.nextFloat() - 0.5) * 0.5;
                double ySpeed = (random.nextFloat() - 0.5) * 0.5;
                double zSpeed = (random.nextFloat() - 0.5) * 0.5;
                int j = random.nextInt(2) * 2 - 1;
                if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                    x = pos.getX() + 0.5 + 0.25 * j;
                    xSpeed = random.nextFloat() * 2.0F * j;
                } else {
                    z = pos.getZ() + 0.5 + 0.25 * j;
                    zSpeed = random.nextFloat() * 2.0F * j;
                }
                level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch ((Direction.Axis) state.getValue(AXIS)) {
            case Z -> Z_AXIS_AABB;
            default -> X_AXIS_AABB;
        };
    }




    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    public static ResourceKey<Level> destinationDimension() {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY,new ResourceLocation(EnderDimensions.REALM_KEY.location().toString()));
    }


    public static ResourceKey<Level> returnDimension() {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(Level.END.location().toString()));
    }


}
