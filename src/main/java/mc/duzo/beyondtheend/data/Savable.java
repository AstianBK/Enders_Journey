package mc.duzo.beyondtheend.data;

import net.minecraft.nbt.CompoundTag;

public interface Savable {
	CompoundTag serialise();
	void deserialise(CompoundTag data);
}
