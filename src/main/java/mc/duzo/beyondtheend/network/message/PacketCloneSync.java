package mc.duzo.beyondtheend.network.message;


import com.google.common.primitives.Ints;
import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class PacketCloneSync implements Packet<PacketListener> {
    private final List<ResourceLocation> location;
    public PacketCloneSync(FriendlyByteBuf buf) {
        this.location = buf.readCollection(PacketCloneSync::newListWithExpectedSize,this::readList);
    }

    public static <K extends @Nullable Object, V extends @Nullable Object>
    List<V> newListWithExpectedSize(int expectedSize) {
        return new ArrayList<>(capacity(expectedSize));
    }
    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            // This is the calculation used in JDK8 to resize when a putAll
            // happens; it seems to be the most conservative calculation we
            // can make.  0.75 is the default load factor.
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE; // any large value
    }
    static int checkNonnegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
        }
        return value;
    }
    public ResourceLocation readList(FriendlyByteBuf buf){
        return buf.readResourceLocation();
    }

    public void writeList(FriendlyByteBuf buf,ResourceLocation location){
        buf.writeResourceLocation(location);
    }

    public PacketCloneSync(List<ResourceLocation> location) {
        this.location = location;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.location,this::writeList);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::setList);
        context.get().setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    public void setList(){
        Player player=Minecraft.getInstance().player;
        assert player!=null;
        var portalPlayer=PortalPlayer.get(player).orElse(null);
        portalPlayer.getList().clear();
        portalPlayer.setListEye(this.location);

    }


    @Override
    public void handle(PacketListener p_131342_) {

    }
}
