package mc.duzo.ender_journey.network.message;


import mc.duzo.ender_journey.capabilities.PortalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSync implements Packet<PacketListener> {
    private final int eye;
    private final boolean isVisitForest;
    public PacketSync(FriendlyByteBuf buf) {
        this.eye =buf.readInt();
        this.isVisitForest = buf.readBoolean();
    }

    public PacketSync(int pos,boolean isVisitForest) {
        this.eye =pos;
        this.isVisitForest = isVisitForest;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.eye);
        buf.writeBoolean(this.isVisitForest);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::clientHandle);
        context.get().setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    public void clientHandle(){
        Player player=Minecraft.getInstance().player;
        assert player!=null;
        var portalPlayer=PortalPlayer.get(player).orElse(null);
        portalPlayer.setEyesEarn(this.eye);
        portalPlayer.setVisitTwilightForest(this.isVisitForest);
    }


    @Override
    public void handle(PacketListener p_131342_) {

    }
}
