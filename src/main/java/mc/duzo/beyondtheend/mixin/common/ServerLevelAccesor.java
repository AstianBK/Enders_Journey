package mc.duzo.beyondtheend.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerLevelAccesor {
    @Mutable
    @Accessor("serverLevelData")
    public void setServerLevelData(ServerLevelData data);

    @Accessor("serverLevelData")
    public ServerLevelData get$ServerLevelData();

}
