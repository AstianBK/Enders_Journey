package mc.duzo.beyondtheend;

import com.klikli_dev.occultism.registry.OccultismBlocks;
import mc.duzo.beyondtheend.capabilities.BkCapabilities;
import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import mc.duzo.beyondtheend.capabilities.PortalPlayerCapability;
import mc.duzo.beyondtheend.common.DimensionUtil;
import mc.duzo.beyondtheend.mixin.common.AdvancementsProgressAccessor;
import mc.duzo.beyondtheend.network.PacketHandler;
import mc.duzo.beyondtheend.network.message.PacketSync;
import mc.duzo.beyondtheend.network.message.PacketUpdateChuck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = EndersJourney.MODID)
public class Events {

    @SubscribeEvent
    public static void onPlayerLoginDimension(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        DimensionUtil.startInBEL(player);
    }
    @SubscribeEvent
    public static void onAdvancementRevoke(AdvancementEvent.AdvancementProgressEvent event){
        if(event.getProgressType()== AdvancementEvent.AdvancementProgressEvent.ProgressType.REVOKE){
            Player player = event.getEntity();
            PortalPlayer.get(player).ifPresent(portalPlayer -> {
                if(player instanceof ServerPlayer player1){
                    int eyes=DimensionUtil.getEyesEarn(((AdvancementsProgressAccessor)player1.getAdvancements()).list(),portalPlayer);
                    portalPlayer.setEyesEarn(eyes);
                    portalPlayer.setListEye(new ArrayList<>());
                    DimensionUtil.getEyesEarn(((AdvancementsProgressAccessor)player1.getAdvancements()).list(),portalPlayer);
                    if(!player.level.isClientSide){
                        PacketHandler.sendToPlayer(new PacketSync(eyes), player1);
                        PacketHandler.sendToPlayer(new PacketUpdateChuck(),player1);
                    }
                }

            });
        }
    }
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event){
        if(DimensionUtil.eyesLocation.contains(event.getAdvancement().getId())){
            PortalPlayer.get(event.getEntity()).ifPresent(portalPlayer -> {
                if(!portalPlayer.getList().contains(event.getAdvancement().getId())){
                    event.getEntity().level.players().forEach(player -> {
                        PortalPlayer.get(player).ifPresent(portalPlayer1 -> {
                            portalPlayer.plusEye(event.getAdvancement().getId());
                            giveHeartContainer(player);
                        });
                    });
                    placeOrReloadStorage(portalPlayer.getPlayer().level,portalPlayer.getEyesEarn());
                }
            });
        }
    }

    private static void placeOrReloadStorage(Level level,int eyeEarn) {
        if(!level.isClientSide){
            BlockState prevState=level.getBlockState(new BlockPos(0 ,114, 31));
            BlockState state=getBlockStateForEye(eyeEarn);
            boolean flag=prevState.isAir() || newStateIsDowngrade(prevState,state);
            if(state!=null && flag){
                level.setBlock(new BlockPos(0 ,114, 31),state.setValue(BlockStateProperties.FACING,Direction.DOWN),3);
            }
        }
    }

    private static boolean newStateIsDowngrade(BlockState prevState, BlockState state) {
        if(state==null){
            return false;
        }
        int idPrevState=getIdForState(prevState);
        int idNewState=getIdForState(state);
        return idPrevState<idNewState;
    }

    private static int getIdForState(BlockState prevState) {
        String name = ForgeRegistries.BLOCKS.getKey(prevState.getBlock()).toString();
        return name.split(":")[1].charAt(23);
    }

    private static BlockState getBlockStateForEye(int eyeEarn) {
        switch (eyeEarn){
            case 3->{
                return OccultismBlocks.STORAGE_STABILIZER_TIER1.get().defaultBlockState();
            }
            case 7->{
                return OccultismBlocks.STORAGE_STABILIZER_TIER2.get().defaultBlockState();
            }
            case 13->{
                return OccultismBlocks.STORAGE_STABILIZER_TIER3.get().defaultBlockState();
            }
            case 18->{
                return OccultismBlocks.STORAGE_STABILIZER_TIER4.get().defaultBlockState();
            }
        }
        return null;
    }

    private static void giveHeartContainer(Player player) {
        Item heart=PortalPlayerCapability.getItem(new ResourceLocation("paraglider","heart_container"));
        if(heart!=null){
            if (!player.level.isClientSide) {
                ItemStack stack=new ItemStack(heart);
                if(!player.getAbilities().instabuild){
                    if (!player.addItem(stack)) {
                        player.spawnAtLocation(new ItemStack(heart));
                    }
                }
            }
        }else {
            EndersJourney.LOGGER.debug("Not found item");
        }
    }

    @SubscribeEvent
    public static void attachEntityCapability(AttachCapabilitiesEvent<Entity> event){
        if(event.getObject() instanceof LivingEntity){
            PortalPlayerCapability oldCap = BkCapabilities.getEntityPatch(event.getObject(), PortalPlayerCapability.class);
            if(oldCap==null){
                if(event.getObject() instanceof Player player){
                    PortalPlayerCapability.PortalPlayerProvider prov=new PortalPlayerCapability.PortalPlayerProvider();
                    PortalPlayer cap = prov.getCapability(BkCapabilities.PORTAL_PLAYER_CAPABILITY,null).orElse(null);
                    cap.setPlayer(player);
                    event.addCapability(new ResourceLocation(EndersJourney.MODID,"portal"),prov);
                }
            }
        }
    }


    @SubscribeEvent
    public static void onInteractWithPortalFrame(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        Direction direction = event.getFace();
        ItemStack itemStack = event.getItemStack();
        InteractionHand interactionHand = event.getHand();
        if (DimensionUtil.createPortal(player, level, blockPos, direction, itemStack, interactionHand)) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public static void onWaterExistsInsidePortalFrame(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);
        FluidState fluidState = level.getFluidState(blockPos);
        if (DimensionUtil.detectWaterInFrame(level, blockPos, blockState, fluidState)) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        Level level = event.level;
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            DimensionUtil.tickTime(level);
        }
    }


    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        ResourceKey<Level> dimension = event.getDimension();
        DimensionUtil.dimensionTravel(entity, dimension);
    }


    @SubscribeEvent
    public static void onPlayerTraveling(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        DimensionUtil.travelling(player);
    }


    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor level = event.getLevel();
        DimensionUtil.initializeLevelData(level);
    }



    @SubscribeEvent
    public static void onSleepFinish(SleepFinishedTimeEvent event) {
        LevelAccessor level = event.getLevel();
        Long time = DimensionUtil.finishSleep(level, event.getNewTime());
        if (time != null) {
            event.setTimeAddition(time);
        }
    }


    @SubscribeEvent
    public static void onTriedToSleep(SleepingTimeCheckEvent event) {
        Player player = event.getEntity();
        if (DimensionUtil.isEternalDay(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PortalPlayer.get(player).ifPresent(portalPlayer -> {
            portalPlayer.setPlayer(player);
            if(player instanceof ServerPlayer player1){
                int eyes=DimensionUtil.getEyesEarn(((AdvancementsProgressAccessor)player1.getAdvancements()).list(),portalPlayer);
                portalPlayer.setEyesEarn(eyes);
                if(!player.level.isClientSide){
                    PacketHandler.sendToPlayer(new PacketSync(eyes), (ServerPlayer) player);
                }
            }
        });
    }


    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
    }


    @SubscribeEvent
    public static void onPlayerJoinLevel(EntityJoinLevelEvent event) {
        Entity player = event.getEntity();
    }


    @SubscribeEvent
    public static void onPlayerUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            PortalPlayer.get(player).ifPresent(PortalPlayer::onUpdate);
        }
    }


    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        PortalPlayer.get(player).ifPresent(portalPlayer -> {
            portalPlayer.setPlayer(player);
            if(player instanceof ServerPlayer player1){
                int eyes=DimensionUtil.getEyesEarn(((AdvancementsProgressAccessor)player1.getAdvancements()).list(),portalPlayer);
                portalPlayer.setEyesEarn(eyes);
                if(!player.level.isClientSide){
                    PacketHandler.sendToPlayer(new PacketSync(eyes), (ServerPlayer) player);
                }
            }
        });
    }


    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        PortalPlayer.get(player).ifPresent(portalPlayer -> {
            portalPlayer.setPlayer(player);
            if(player instanceof ServerPlayer player1){
                int eyes=DimensionUtil.getEyesEarn(((AdvancementsProgressAccessor)player1.getAdvancements()).list(),portalPlayer);
                portalPlayer.setEyesEarn(eyes);

                if(!player.level.isClientSide){
                    PacketHandler.sendToPlayer(new PacketSync(eyes), (ServerPlayer) player);
                }
            }
        });
    }



}
