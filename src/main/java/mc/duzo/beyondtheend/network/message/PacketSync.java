package mc.duzo.beyondtheend.network.message;


import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSync implements Packet<PacketListener> {
    private final int eye;
    public PacketSync(FriendlyByteBuf buf) {
        this.eye =buf.readInt();
    }

    public PacketSync(int pos) {
        this.eye =pos;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.eye);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->{
            Player player=Minecraft.getInstance().player;
            assert player!=null;
            var portalPlayer=PortalPlayer.get(player).orElse(null);
            portalPlayer.setEyesEarn(this.eye);
        });
        context.get().setPacketHandled(true);
    }


    @Override
    public void handle(PacketListener p_131342_) {

    }
}
