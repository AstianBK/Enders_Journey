package mc.duzo.ender_journey.capabilities;

import com.TBK.beyondtheend.common.registry.BkCommonRegistry;
import com.google.common.collect.Queues;
import com.mojang.math.Vector3d;
import mc.duzo.ender_journey.EndersJourney;
import mc.duzo.ender_journey.common.DimensionUtil;
import mc.duzo.ender_journey.common.block_entity.ColumnBlockEntity;
import mc.duzo.ender_journey.common.register.BKBlocks;
import mc.duzo.ender_journey.mixin.common.AdvancementsProgressAccessor;
import mc.duzo.ender_journey.network.PacketHandler;
import mc.duzo.ender_journey.network.message.PacketCloneSync;
import mc.duzo.ender_journey.network.message.PacketSync;
import mc.duzo.ender_journey.network.message.PacketUpdateChuck;
import mc.duzo.ender_journey.world.dimension.EnderDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class PortalPlayerCapability implements PortalPlayer{
    Player player;
    public int portalTime=0;
    private float prevPortalAnimTime;
    private float portalAnimTime;
    private boolean isInPortal;

    private boolean visitTwilightForest = false;
    private int eyesEarn;
    private List<ResourceLocation> eyesEarns=new ArrayList<>();

    public void setPlayer(Player player){
        this.player=player;
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public int getEyesEarn() {
        return this.eyesEarn;
    }

    @Override
    public void setEyesEarn(int value) {
        this.eyesEarn=value;
    }

    @Override
    public boolean visitTwilightForest() {
        return visitTwilightForest;
    }

    public void addEye(ResourceLocation location){
        this.eyesEarns.add(location);
        if(this.player instanceof ServerPlayer){
            PacketHandler.sendToPlayer(new PacketCloneSync(this.eyesEarns), (ServerPlayer) player);
        }
    }

    public void sync() {
        PacketHandler.sendToPlayer(new PacketSync(this.eyesEarn,this.visitTwilightForest()), (ServerPlayer) player);
    }

    @Override
    public void plusEye(ResourceLocation location) {
        if(this.eyesEarn++>24){
            this.eyesEarn=24;
        }

        if(!player.level.isClientSide){
            if(EndersJourney.getServer()!=null){
                ServerLevel level=EndersJourney.getServer().getLevel(EnderDimensions.REALM_KEY);
                if(level!=null){
                    PacketHandler.sendToPlayer(new PacketUpdateChuck(), (ServerPlayer) this.player);

                    Item stack=getItem(location);
                    BlockPos pos =null;
                    for(Map.Entry<ResourceLocation,BlockPos> posEntry:DimensionUtil.eyeItemForBlockPos.entrySet()){
                        ItemStack stack1 = DimensionUtil.getItem(posEntry.getKey());
                        ColumnBlockEntity block= (ColumnBlockEntity) level.getBlockEntity(posEntry.getValue());
                        if(block!=null && stack1.is(stack)){
                            block.setChanged();
                        }
                    }

                }

            }


            PacketHandler.sendToPlayer(new PacketSync(this.eyesEarn,this.visitTwilightForest()), (ServerPlayer) player);
        }
        this.addEye(location);
    }

    @Override
    public void setVisitTwilightForest(boolean visitTwilightForest) {
        this.visitTwilightForest = visitTwilightForest;
    }


    public static Item getItem(ResourceLocation location) {
        Holder<Item> holder=ForgeRegistries.ITEMS.getHolder(location).orElse(null);
        if(holder!=null){
            return holder.get();
        }
        return Items.AIR;
    }

    @Override
    public void setListEye(List<ResourceLocation> list) {
        this.eyesEarns=list;
        if(this.player instanceof ServerPlayer){
            PacketHandler.sendToPlayer(new PacketCloneSync(list), (ServerPlayer) player);
        }
    }

    @Override
    public List<ResourceLocation> getList() {
        return this.eyesEarns;
    }

    @Override
    public void onLogout() {

    }

    @Override
    public void onLogin() {

    }

    @Override
    public void onJoinLevel() {

    }

    @Override
    public void copyFrom(PortalPlayer capability, Player player, Player newPlayer) {

        this.setPlayer(newPlayer);
        this.setEyesEarn(capability.getEyesEarn());
    }

    @Override
    public void onUpdate() {
        if (this.player.getY()<0 && player.level.dimension() == EnderDimensions.REALM_KEY){
            int chunkX = player.chunkPosition().x;
            int chunkZ = player.chunkPosition().z;
            if (chunkX > 300 && chunkX < 320 && chunkZ < 10 && chunkZ > -10){
                this.player.fallDistance = 0.0F;
                this.player.teleportTo(5000,152,8);
            }

        }
    }

    @Override
    public void setCanSpawnInAether(boolean canSpawnInAether) {

    }

    @Override
    public boolean canSpawnInAether() {
        return false;
    }

    @Override
    public void givePortalItem() {

    }

    @Override
    public void setCanGetPortal(boolean canGetPortal) {

    }

    @Override
    public boolean canGetPortal() {
        return false;
    }

    @Override
    public void setInPortal(boolean inPortal) {
        this.isInPortal=inPortal;
    }

    @Override
    public boolean isInPortal() {
        return this.isInPortal;
    }

    @Override
    public void setPortalTimer(int timer) {
        this.portalTime=timer;
    }

    @Override
    public int getPortalTimer() {
        return this.portalTime;
    }

    @Override
    public float getPortalAnimTime() {
        return this.portalAnimTime;
    }

    @Override
    public float getPrevPortalAnimTime() {
        return this.prevPortalAnimTime;
    }

    @Override
    public void setHitting(boolean isHitting) {

    }

    @Override
    public boolean isHitting() {
        return false;
    }

    @Override
    public void setMoving(boolean isMoving) {

    }

    @Override
    public boolean isMoving() {
        return false;
    }

    @Override
    public void setJumping(boolean isJumping) {

    }

    @Override
    public boolean isJumping() {
        return false;
    }

    @Override
    public boolean haveEye(ResourceLocation location) {
        return this.eyesEarns.contains(location);
    }

    @Override
    public int checkEyes(Level level) {
        return DimensionUtil.checkEyeEarnForAdv(((AdvancementsProgressAccessor)((ServerPlayer)this.player).getAdvancements()).list(),level,this);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag=new CompoundTag();
        tag.putInt("eyes",this.getEyesEarn());
        if(!this.eyesEarns.isEmpty()){
            ListTag listTag=new ListTag();
            for(ResourceLocation location : this.eyesEarns){
                CompoundTag tag1=new CompoundTag();
                tag1.putString("id",location.toString());
                listTag.add(tag1);
            }
            tag.put("eyesEarns",listTag);
        }
        tag.putBoolean("visitTwilightForest",this.visitTwilightForest);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.setEyesEarn(nbt.getInt("eyes"));
        if(nbt.contains("eyesEarns",9)){
            ListTag tags=nbt.getList("eyesEarns",10);
            for(int i=0;i<tags.size();i++){
                CompoundTag tag=tags.getCompound(i);
                this.eyesEarns.add(new ResourceLocation(tag.getString("id")));
            }
        }
        this.visitTwilightForest = nbt.getBoolean("visitTwilightForest");
    }

    private void handleAetherPortal() {
        if (this.getPlayer().level.isClientSide()) {
            this.prevPortalAnimTime = this.portalAnimTime;
            Minecraft minecraft = Minecraft.getInstance();
            if (this.isInPortal) {
                if (minecraft.screen != null && !minecraft.screen.isPauseScreen()) {
                    if (minecraft.screen instanceof AbstractContainerScreen) {
                        this.getPlayer().closeContainer();
                    }
                }

                if (this.getPortalAnimTime() == 0.0F && minecraft.level!=null) {
                    minecraft.level.playSound(null,this.player, SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT,2.0F,1.0F);
                }
            }
        }

        if (this.isInPortal()) {
            ++this.portalTime;
            if (this.getPlayer().level.isClientSide()) {
                this.portalAnimTime += 0.0125F;
                if (this.getPortalAnimTime() > 1.0F) {
                    this.portalAnimTime = 1.0F;
                }
            }
            this.isInPortal = false;
        }
        else {
            if (this.getPlayer().level.isClientSide()) {
                if (this.getPortalAnimTime() > 0.0F) {
                    this.portalAnimTime -= 0.05F;
                }

                if (this.getPortalAnimTime() < 0.0F) {
                    this.portalAnimTime = 0.0F;
                }
            }
            if (this.getPortalTimer() > 0) {
                this.portalTime -= 4;
            }
        }
    }
    public static boolean isChunkUnlocked(Player player, int indexX,int indexZ){
        IZoneChunkCapability cap = BkCapabilities.getWorldCapability(player.level, IZoneChunkCapability.class);
        if (cap != null){

            return cap.unlockChunk(indexX,indexZ);
        }

        return false;
    }
    public static void createChunkGlowing(ServerLevel level,int x,int y,int z){
        for (int x1 = -8 ; x1 < 8 ; x1++){
            for (int z1 = -8 ; z1 < 8 ; z1++){
                level.setBlock(new BlockPos(x+x1,y,z+z1), BkCommonRegistry.GLOWING_ENERGY_ROCK.get().defaultBlockState(),3);
            }
        }
    }


    public static class PortalPlayerProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final LazyOptional<PortalPlayer> instance=LazyOptional.of(PortalPlayerCapability::new);

        @NonNull
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BkCapabilities.PORTAL_PLAYER_CAPABILITY.orEmpty(cap,instance.cast());
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.orElseThrow(NullPointerException::new).serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.orElseThrow(NullPointerException::new).deserializeNBT(nbt);
        }
    }
}
