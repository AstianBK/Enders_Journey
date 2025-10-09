package mc.duzo.ender_journey.data;

import net.minecraft.nbt.CompoundTag;

public interface Savable {
	CompoundTag serialise();
	void deserialise(CompoundTag data);
}
