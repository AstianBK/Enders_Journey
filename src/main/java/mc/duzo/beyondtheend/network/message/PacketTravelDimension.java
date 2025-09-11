package mc.duzo.beyondtheend.network.message;

import mc.duzo.beyondtheend.common.DimensionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTravelDimension implements Packet<PacketListener> {
    private final boolean flag;
    public PacketTravelDimension(FriendlyByteBuf buf) {
        this.flag = buf.readBoolean();
    }

    public PacketTravelDimension(boolean flag) {
        this.flag=flag;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.flag);

    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->{
            assert context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT;
            handlerAnim();
        });
        context.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handlerAnim() {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            DimensionUtil.displayAetherTravel = this.flag;
        }
    }

    @Override
    public void handle(PacketListener p_131342_) {

    }
}
