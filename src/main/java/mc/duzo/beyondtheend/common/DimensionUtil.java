package mc.duzo.beyondtheend.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import mc.duzo.beyondtheend.common.blocks.BKPortalForcer;
import mc.duzo.beyondtheend.common.blocks.PortalBlock;
import mc.duzo.beyondtheend.mixin.common.ServerGamePacketListenerImplAccessor;
import mc.duzo.beyondtheend.mixin.common.ServerLevelAccesor;
import mc.duzo.beyondtheend.network.PacketHandler;
import mc.duzo.beyondtheend.network.message.PacketLeavingDimension;
import mc.duzo.beyondtheend.network.message.PacketSync;
import mc.duzo.beyondtheend.network.message.PacketTravelDimension;
import mc.duzo.beyondtheend.world.dimension.EnderDimensions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DimensionUtil {
    public static boolean playerLeavingAether;
    public static boolean displayAetherTravel;
    public static int teleportationTimer;
    public static final ImmutableMap<ItemStack,BlockPos> eyeItemForBlockPos = ImmutableMap.ofEntries(
            buildEntry(buildItem("black_eye"),new BlockPos(31,54,-12)),
            buildEntry(buildItem("witch_eye"),new BlockPos(31,54,12)),
            buildEntry(buildItem("cold_eye"),new BlockPos(38,54,-9)),
            buildEntry(buildItem("corrupted_eye"),new BlockPos(35,54,-12)),
            buildEntry(buildItem("lost_eye"),new BlockPos(40,54,-6)),
            buildEntry(buildItem("nether_eye"),new BlockPos(41,55,-3)),
            buildEntry(buildItem("rogue_eye"),new BlockPos(41,56,0)),
            buildEntry(buildItem("cursed_eye"),new BlockPos(41,55,3)),
            buildEntry(buildItem("evil_eye"),new BlockPos(40,54,6)),
            buildEntry( buildItem("guardian_eye"),new BlockPos(38,54,9)),
            buildEntry(buildItem("magical_eye"),new BlockPos(35,54,12)),
            buildEntry(buildItem("undead_eye"),new BlockPos(33,52,0)),
            buildEntry(buildItem("exotic_eye"),new BlockPos(-31,54,12)),
            buildEntry(buildItem("carminite_eye"),new BlockPos(-31,54,-12)),
            buildEntry(buildItem("aurora_eye"),new BlockPos(-38,54,-9)),
            buildEntry(buildItem("fiery_eye"),new BlockPos(-35,54,-12)),
            buildEntry(buildItem("abyss_eye"),new BlockPos(-40,54,-6)),
            buildEntry(buildItem("mech_eye"),new BlockPos(-41,55,-3)),
            buildEntry(buildItem("monstrous_eye"),new BlockPos(-41,56,0)),
            buildEntry(buildItem("void_eye"),new BlockPos(-41,55,3)),
            buildEntry(buildItem("flame_eye"),new BlockPos(-40,54,6)),
            buildEntry(buildItem("parasite_eye"),new BlockPos(-38,54,9)),
            buildEntry(buildItem("desert_eye"),new BlockPos(-35,54,12)),
            buildEntry(buildItem("sculk_eye"),new BlockPos(-33,52,0))
            );
    public static Map.Entry<ItemStack,BlockPos> buildEntry(ItemStack eye,BlockPos pos){
        return Map.entry(eye,pos);
    }
    public static final List<ResourceLocation> eyesLocation=List.of(build("black_eye"),
            build("cold_eye"),build("corrupted_eye"),build("lost_eye"),build("nether_eye"),
            build("rogue_eye"),build("cursed_eye"),build("evil_eye"),build("guardian_eye"),
            build("magical_eye"),build("witch_eye"),build("undead_eye"),build("exotic_eye"),
            build("carminite_eye"),build("aurora_eye"),build("fiery_eye"),build("abyss_eye"),
            build("mech_eye"),build("monstrous_eye"),build("void_eye"),build("flame_eye"),
            build("parasite_eye"),build("desert_eye"),build("sculk_eye"));
    public static ResourceLocation build(String name){
        return new ResourceLocation("endrem","main/"+name);
    }
    public static ItemStack buildItem(String name){
        Holder<Item> holder= ForgeRegistries.ITEMS.getHolder(new ResourceLocation("endrem",name)).orElse(null);
        if(holder!=null){
            return new ItemStack(holder.get());
        }
        EndersJourney.LOGGER.debug("Item no found "+name);
        return ItemStack.EMPTY;
    }
    public static int getEyesEarn(Map<Advancement, AdvancementProgress> map,PortalPlayer portalPlayer){
        List<Advancement> advancements=new ArrayList<>();
        List<ResourceLocation> resourceLocations=new ArrayList<>();
        map.forEach(((advancement, advancementProgress) -> {
            if(eyesLocation.contains(advancement.getId()) && advancementProgress.isDone()){
                advancements.add(advancement);
                resourceLocations.add(advancement.getId());
            }
        }));
        portalPlayer.setListEye(resourceLocations);
        return advancements.size();
    }
    public static int checkEyeEarnForAdv(Map<Advancement, AdvancementProgress> map,Level level,PortalPlayer player){
        int eyeForAdv=getEyesEarn(map,player);
        int eyeEarn=player.getEyesEarn();
        if(eyeEarn!=eyeForAdv){
            player.setEyesEarn(eyeForAdv);
            if(!level.isClientSide){
                PacketHandler.sendToPlayer(new PacketSync(eyeForAdv), (ServerPlayer) player.getPlayer());
            }
            return eyeForAdv;
        }else {
            return eyeEarn;
        }
    }
    public static void startInBEL(Player player) {
        PortalPlayer.get(player).ifPresent(portalPlayer->{
            if (portalPlayer.canSpawnInAether()) { // Checks if the player has been set to spawn in the Aether.
                if (player instanceof ServerPlayer serverPlayer) {
                    MinecraftServer server = serverPlayer.level.getServer();
                    if (server != null) {
                        ServerLevel aetherLevel = server.getLevel(EnderDimensions.REALM_KEY);
                        if (aetherLevel != null && serverPlayer.level.dimension() != EnderDimensions.REALM_KEY) {
                            if (player.changeDimension(aetherLevel, new BKPortalForcer(aetherLevel, false, true)) != null) {
                                serverPlayer.setRespawnPosition(EnderDimensions.REALM_KEY, serverPlayer.blockPosition(), serverPlayer.getYRot(), true, false);
                                portalPlayer.setCanSpawnInAether(false); // Sets that the player has already spawned in the Aether.
                            }
                        }
                    }
                }
            }
        });
    }


    public static boolean createPortal(Player player, Level level, BlockPos pos, @Nullable Direction direction, ItemStack stack, InteractionHand hand) {
        if (direction != null) {
            BlockPos relativePos = pos.relative(direction);
            if (stack.is(Items.FLINT_AND_STEEL)) { // Checks if the item can activate the portal.
                if ((level.dimension() == PortalBlock.returnDimension() || level.dimension() == PortalBlock.destinationDimension())) {
                    Optional<PortalShape> optional = PortalShape.findEmptyAetherPortalShape(level, relativePos, Direction.Axis.X);
                    if (optional.isPresent()) {
                        optional.get().createPortalBlocks();
                        player.playSound(SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
                        player.swing(hand);
                        if (!player.isCreative()) {
                            if (stack.getCount() > 1) {
                                stack.shrink(1);
                                player.addItem(stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY);
                            } else if (stack.isDamageableItem()) {
                                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                            } else {
                                player.setItemInHand(hand, stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean detectWaterInFrame(LevelAccessor levelAccessor, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (levelAccessor instanceof Level level) {
            if (fluidState.is(Fluids.WATER) && fluidState.createLegacyBlock().getBlock() == blockState.getBlock()) {
                if ((level.dimension() == PortalBlock.returnDimension() || level.dimension() == PortalBlock.destinationDimension()) ) {
                    Optional<PortalShape> optional = PortalShape.findEmptyAetherPortalShape(level, pos, Direction.Axis.X);
                    if (optional.isPresent()) {
                        optional.get().createPortalBlocks();
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static void tickTime(Level level) {

    }




    public static void dimensionTravel(Entity entity, ResourceKey<Level> dimension) {
        if (entity instanceof Player player) {
            PortalPlayer.get(player).ifPresent(player1->{
                if (entity.level.dimension() == PortalBlock.destinationDimension() && dimension == PortalBlock.returnDimension()) { // We display the Descending GUI text to the player if they're about to return to the Overworld.
                    displayAetherTravel = true;
                    playerLeavingAether = true;
                    PacketHandler.sendToAllTracking(new PacketTravelDimension(true),player);
                    PacketHandler.sendToAllTracking(new PacketLeavingDimension(true),player);
                } else if (entity.level.dimension() == PortalBlock.returnDimension() && dimension == PortalBlock.destinationDimension()) { // We display the Ascending GUI text to the player if they're about to enter the Aether.
                    displayAetherTravel = true;
                    playerLeavingAether = false;
                    PacketHandler.sendToAllTracking(new PacketTravelDimension(true),player);
                    PacketHandler.sendToAllTracking(new PacketLeavingDimension(false),player);
                } else { // Don't display any text if not travelling between the Aether and Overworld or vice-versa.
                    displayAetherTravel = false;
                    PacketHandler.sendToAllTracking(new PacketTravelDimension(false),player);
                }
            });
        }
    }


    public static void travelling(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (teleportationTimer > 0) { // Prevents the player from being kicked for flying.
                ServerGamePacketListenerImplAccessor serverGamePacketListenerImplAccessor = (ServerGamePacketListenerImplAccessor) serverPlayer.connection;
                serverGamePacketListenerImplAccessor.getAboveTicksCount$(0);
                serverGamePacketListenerImplAccessor.get$AboveGroundVehicleTickCount(0);
                teleportationTimer--;
            }
            if (teleportationTimer < 0 || serverPlayer.verticalCollisionBelow) {
                teleportationTimer = 0;
            }
        }
    }

    public static void initializeLevelData(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel && serverLevel.dimensionType().effectsLocation().equals(EnderDimensions.REALM_KEY.location())) {

        }
    }


    @Nullable
    public static Long finishSleep(LevelAccessor level, long newTime) {
        if (level instanceof ServerLevel && level.dimensionType().effectsLocation().equals(EnderDimensions.REALM_KEY.location())) {
            ServerLevelAccessor serverLevelAccessor = (ServerLevelAccessor) level;
            ServerLevelAccesor accessor = (ServerLevelAccesor) serverLevelAccessor;
            accessor.get$ServerLevelData().setRainTime(0);
            accessor.get$ServerLevelData().setRaining(false);
            accessor.get$ServerLevelData().setThunderTime(0);
            accessor.get$ServerLevelData().setThundering(false);

            long time = newTime + 48000L;
            return time - time % (long) (24000) * 3;
        }
        return null;
    }


    public static boolean isEternalDay(Player player) {
        if (player.level.dimensionType().effectsLocation().equals(EnderDimensions.REALM_KEY.location())) {
            return true;
        }
        return false;
    }

    public static boolean haveEye(Player player,Map<Advancement, AdvancementProgress> map,ItemStack eye) {
        ResourceLocation resourceLocation=ForgeRegistries.ITEMS.getKey(eye.getItem());
        List<ResourceLocation> location=new ArrayList<>();
        if(resourceLocation!=null){
            map.forEach(((advancement, advancementProgress) -> {
                if(resourceLocation.equals(advancement.getId()) && advancementProgress.isDone()){
                    location.add(resourceLocation);
                }
            }));
        }
        return !location.isEmpty();
    }
}
