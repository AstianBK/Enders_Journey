package mc.duzo.beyondtheend.common.block_entity;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.common.register.BKBlockEntity;
import mc.duzo.beyondtheend.network.PacketHandler;
import mc.duzo.beyondtheend.network.message.PacketCloneSync;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ColumnBlockEntity extends BlockEntity {
    private ItemStack eye=ItemStack.EMPTY;
    public ColumnBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
            super(BKBlockEntity.COLUMN_ENTITY.get(), p_155229_, p_155230_);
    }

    public void setItem(ItemStack stack){
        this.eye=stack;

        this.setChanged();
    }

    public ItemStack getItem(){
        return this.eye;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if(!this.getItem().isEmpty()){
            CompoundTag tag1=new CompoundTag();
            this.getItem().save(tag1);
            tag.put("eye",tag1);
        }
    }

    public void setChanged() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);

            super.setChanged();

        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if(this.level!=null && this.level.isClientSide){
            EndersJourney.LOGGER.debug("Entro por cliente");

            if(pkt.getTag()!=null){
                ItemStack stack =ItemStack.of(pkt.getTag().getCompound("eye"));
                EndersJourney.LOGGER.debug("El item en el compound es :" + stack);
            }else {
                EndersJourney.LOGGER.debug("El tag es null");
            }
        }else{
            EndersJourney.LOGGER.debug("Entro por server");
        }
        super.onDataPacket(net, pkt);
    }

    @Override
    public void load(CompoundTag p_155245_) {
        super.load(p_155245_);
        if(p_155245_.contains("eye",10)){
            this.eye= ItemStack.of(p_155245_.getCompound("eye"));
        }
    }


}
