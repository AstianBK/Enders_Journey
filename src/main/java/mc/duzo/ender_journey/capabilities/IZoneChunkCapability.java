package mc.duzo.ender_journey.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IZoneChunkCapability extends INBTSerializable<CompoundTag> {
    boolean unlockChunk(int indexX,int indexY);
    void addChunk(int indexX,int indexY);
}
