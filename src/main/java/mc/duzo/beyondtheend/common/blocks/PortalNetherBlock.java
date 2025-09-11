package mc.duzo.beyondtheend.common.blocks;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.capabilities.BkCapabilities;
import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import mc.duzo.beyondtheend.common.DimensionUtil;
import mc.duzo.beyondtheend.mixin.common.EntityAccessor;
import mc.duzo.beyondtheend.world.dimension.EnderDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public class PortalNetherBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public PortalNetherBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState pState=this.defaultBlockState();
        switch (pContext.getHorizontalDirection().getAxis()) {
            case Z:
                return pState.setValue(AXIS, Direction.Axis.X);
            case X:
                return pState.setValue(AXIS, Direction.Axis.Z);
            default:
                return pState;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        EntityAccessor entityAccessor = (EntityAccessor) entity;
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
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
                        if(handler.getEyesEarn()>=8){
                            handler.setInPortal(true);
                            this.handleTeleportation(entity);
                            handler.setPortalTimer(0);
                        }
                    });
                }
            }
        }
    }

    private void handleTeleportation(Entity entity) {
        MinecraftServer server = entity.level.getServer();
        ResourceKey<Level> destinationKey = entity.level.dimension() ==destinationDimension() ? returnDimension() : destinationDimension();
        if (server != null) {
            ServerLevel destinationLevel = server.getLevel(destinationKey);
            if (destinationLevel != null && !entity.isPassenger()) {
                entity.level.getProfiler().push("portal");
                entity.setPortalCooldown();
                entity.changeDimension(destinationLevel, new BKPortalForcer(destinationLevel, true));
                entity.level.getProfiler().pop();
            }
        }
    }


    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null){
            PortalPlayer portalPlayer=PortalPlayer.get(mc.player).orElse((PortalPlayer) null);
            if(portalPlayer!=null){
                boolean isActive=portalPlayer.getEyesEarn()>=8;
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
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch ((Direction.Axis)state.getValue(AXIS)) {
            case Z:
                return Z_AXIS_AABB;
            case X:
            default:
                return X_AXIS_AABB;
        }
    }

    /**
     * Warning for "deprecation" is suppressed because the method is fine to override.
     */
    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return super.updateShape(state, direction, facingState, level, currentPos, facingPos);
    }


    @Override
    public RenderShape getRenderShape(BlockState p_60550_) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null){
            int eyeEarn=PortalPlayer.get(mc.player).orElseGet(null).getEyesEarn();
            if(eyeEarn<8){
                return RenderShape.INVISIBLE;
            }
        }
        return super.getRenderShape(p_60550_);
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
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(Level.NETHER.location().toString()));
    }
}
