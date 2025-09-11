package mc.duzo.beyondtheend.network.message;


import mc.duzo.beyondtheend.capabilities.PortalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketUpdateChuck implements Packet<PacketListener> {
    List<BlockPos> positions=List.of(new BlockPos(-31,89,-1),new BlockPos(-31,89,0)
            ,new BlockPos(0,88,27),new BlockPos(-1,88,27) );
    public PacketUpdateChuck(FriendlyByteBuf buf) {
    }

    public PacketUpdateChuck() {
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->{
            Level level= Minecraft.getInstance().level;
            if(level!=null){
                for (BlockPos pos1:positions){
                    int sectionX = pos1.getX() >> 4; // Divide entre 16
                    int sectionY = pos1.getY() >> 4;  // Divide entre 16
                    int sectionZ = pos1.getZ() >> 4; // Divide entre 16

                    Minecraft.getInstance().levelRenderer.setSectionDirty(sectionX,sectionY,sectionZ);
                }
            }
        });
        context.get().setPacketHandled(true);
    }


    @Override
    public void handle(PacketListener p_131342_) {

    }
}
