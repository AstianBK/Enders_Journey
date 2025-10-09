package mc.duzo.ender_journey.mixin.common;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
