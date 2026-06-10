package mc.duzo.ender_journey.capabilities;

import mc.duzo.ender_journey.EndersJourney;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import twilightforest.util.Vec2i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImagineZoneChunkCapability implements IZoneChunkCapability{
    public List<Vec2i> indexChunks= new ArrayList<>(List.of(new Vec2i(312,0)));
    @Override
    public void addChunk(int x,int z){
        indexChunks.add(new Vec2i(x,z));
    }
    @Override
    public boolean unlockChunk(int indexX, int indexZ) {
        EndersJourney.LOGGER.info("Unlock {}",indexChunks);
        for (Vec2i index : indexChunks){
            if (index.x == indexX && index.z == indexZ){
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (!indexChunks.isEmpty()){
            ListTag tags = new ListTag();
            for(Vec2i vec2i : indexChunks){
                CompoundTag tag = new CompoundTag();
                tag.putInt("x",vec2i.x);
                tag.putInt("z",vec2i.z);
                tags.add(tag);
            }
            nbt.put("Chunks",tags);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if(nbt.contains("Chunks",9)){
            List<Vec2i> vecs = new ArrayList<>();
            ListTag list = nbt.getList("Chunks",10);
            for(int i = 0 ; i < list.size() ; i++){
                CompoundTag tag = list.getCompound(i);
                Vec2i vec2i = new Vec2i(tag.getInt("x"),tag.getInt("z"));
                vecs.add(vec2i);
            }
            indexChunks = vecs;
        }
    }
    public static class ImagineZoneChunkProvider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final LazyOptional<IZoneChunkCapability> instance=LazyOptional.of(ImagineZoneChunkCapability::new);

        @NonNull
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BkCapabilities.CHUNK_CAPABILITY.orEmpty(cap,instance.cast());
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.orElseThrow(NullPointerException::new).serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.orElseThrow(NullPointerException::new).deserializeNBT(nbt);
        }
    }
}
