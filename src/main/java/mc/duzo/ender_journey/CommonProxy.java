package mc.duzo.ender_journey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import tictim.paraglider.ParagliderMod;

import java.util.ArrayList;
import java.util.List;

public class CommonProxy {
    public static final List<PlacedBlock> BLOCKS = new ArrayList<>();

    public void init(){

    }
    public static void initBlocks(){
        add("paraglider:rito_goddess_statue", -10,130,34, Direction.EAST);
        add("paraglider:rito_goddess_statue", 11,130,34, Direction.WEST);
        add("paraglider:rito_goddess_statue", 34,130,10, Direction.NORTH);
        add("paraglider:rito_goddess_statue", 33,130,-10, Direction.SOUTH);
        add("paraglider:rito_goddess_statue", 10,130,-34, Direction.WEST);
        add("paraglider:rito_goddess_statue", -9,130,-34, Direction.EAST);
        add("paraglider:rito_goddess_statue", -34,130,-10, Direction.SOUTH);
        add("paraglider:rito_goddess_statue", -34,130,9, Direction.NORTH);

        // ===== FIREFLIES =====
        add("twilightforest:firefly", -9,129,34, Direction.EAST);
        add("twilightforest:firefly", 10,129,34, Direction.WEST);
        add("twilightforest:firefly", 34,129,9, Direction.NORTH);
        add("twilightforest:firefly", 33,129,-9, Direction.SOUTH);
        add("twilightforest:firefly", 9,129,-34, Direction.WEST);
        add("twilightforest:firefly", -8,129,-34, Direction.EAST);
        add("twilightforest:firefly", -34,129,-9, Direction.SOUTH);
        add("twilightforest:firefly", -34,129,8, Direction.NORTH);

        // ===== FIREFLY SPAWNERS =====
        add("twilightforest:firefly_particle_spawner", -7,125,-22);
        add("twilightforest:firefly_particle_spawner", -12,125,-22);
        add("twilightforest:firefly_particle_spawner", 12,127,-22);
        add("twilightforest:firefly_particle_spawner", 7,127,-22);
        add("twilightforest:firefly_particle_spawner", 22,125,-12);
        add("twilightforest:firefly_particle_spawner", 22,125,-7);
        add("twilightforest:firefly_particle_spawner", 22,127,12);
        add("twilightforest:firefly_particle_spawner", 22,127,7);
        add("twilightforest:firefly_particle_spawner", 12,125,22);
        add("twilightforest:firefly_particle_spawner", 7,125,22);
        add("twilightforest:firefly_particle_spawner", -7,127,22);
        add("twilightforest:firefly_particle_spawner", -12,127,22);
        add("twilightforest:firefly_particle_spawner", -22,125,12);
        add("twilightforest:firefly_particle_spawner", -22,125,7);
        add("twilightforest:firefly_particle_spawner", -22,127,-7);
        add("twilightforest:firefly_particle_spawner", -22,127,-12);

        add("minecraft:air",68,77,46);
    }

    private static void add(String id, int x, int y, int z) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (block != null) BLOCKS.add(new PlacedBlock(block, new BlockPos(x,y,z), null));
    }

    private static void add(String id, int x, int y, int z, Direction facing) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (block != null) BLOCKS.add(new PlacedBlock(block, new BlockPos(x,y,z), facing));
    }

    private static void add(Block block, int x, int y, int z) {
        BLOCKS.add(new PlacedBlock(block, new BlockPos(x,y,z), null));
    }
    public record PlacedBlock(Block block, BlockPos pos, Direction facing) {

    }
}
