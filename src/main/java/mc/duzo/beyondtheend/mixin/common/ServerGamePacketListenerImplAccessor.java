package mc.duzo.beyondtheend.mixin.common;

import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Mutable
    @Accessor("aboveGroundTickCount")
    public void getAboveTicksCount$(int tick);

    @Accessor("aboveGroundVehicleTickCount")
    public void get$AboveGroundVehicleTickCount(int tick);

}
