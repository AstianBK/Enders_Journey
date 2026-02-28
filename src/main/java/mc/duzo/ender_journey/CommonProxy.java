package mc.duzo.ender_journey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

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

        // ===== BLACKSTONE MARCO =====
        for (int x = -3; x <= 3; x++) add(Blocks.BLACKSTONE, x,111,-30);
        for (int z = -31; z >= -36; z--) add(Blocks.BLACKSTONE, 3,111,z);
        for (int x = 2; x >= -3; x--) add(Blocks.BLACKSTONE, x,111,-36);
        for (int z = -35; z >= -31; z--) add(Blocks.BLACKSTONE, -3,111,z);

        // ===== VOID STONE =====
        add("cataclysm:void_stone", -2,111,-31);
        add("cataclysm:void_stone", -1,111,-31);
        add("cataclysm:void_stone", 0,111,-31);
        add("cataclysm:void_stone", 1,111,-31);
        add("cataclysm:void_stone", 2,111,-31);
        add("cataclysm:void_stone", 2,111,-32);
        add("cataclysm:void_stone", 2,111,-33);
        add("cataclysm:void_stone", 2,111,-34);
        add("cataclysm:void_stone", 2,111,-35);
        add("cataclysm:void_stone", 1,111,-35);
        add("cataclysm:void_stone", 0,111,-35);
        add("cataclysm:void_stone", -1,111,-35);
        add("cataclysm:void_stone", -2,111,-35);
        add("cataclysm:void_stone", -2,111,-34);
        add("cataclysm:void_stone", -2,111,-33);
        add("cataclysm:void_stone", -2,111,-32);

        // ===== BLAZE LANTERNS =====
        add("quark:blaze_lantern", -1,111,-32);
        add("quark:blaze_lantern", 0,111,-32);
        add("quark:blaze_lantern", 1,111,-32);
        add("quark:blaze_lantern", 1,111,-33);
        add("quark:blaze_lantern", 1,111,-34);
        add("quark:blaze_lantern", 0,111,-34);
        add("quark:blaze_lantern", -1,111,-34);
        add("quark:blaze_lantern", -1,111,-33);
        add("quark:blaze_lantern", 0,111,-33);

        // ===== LIMPIAR ABAJO (AIR) =====
        for (int x = -3; x <= 3; x++) add(Blocks.AIR, x,52,-30);
        for (int z = -31; z >= -36; z--) add(Blocks.AIR, 3,52,z);
        for (int x = 2; x >= -3; x--) add(Blocks.AIR, x,52,-36);
        for (int z = -35; z >= -31; z--) add(Blocks.AIR, -3,52,z);

        for (int x = -2; x <= 2; x++) add(Blocks.AIR, x,52,-31);
        for (int z = -32; z >= -35; z--) add(Blocks.AIR, 2,52,z);
        for (int x = 1; x >= -2; x--) add(Blocks.AIR, x,52,-35);
        for (int z = -34; z >= -32; z--) add(Blocks.AIR, -2,52,z);

        add(Blocks.AIR, -1,52,-32);
        add(Blocks.AIR, 0,52,-32);
        add(Blocks.AIR, 1,52,-32);
        add(Blocks.AIR, 1,52,-33);
        add(Blocks.AIR, 1,52,-34);
        add(Blocks.AIR, 0,52,-34);
        add(Blocks.AIR, -1,52,-34);
        add(Blocks.AIR, -1,52,-33);
        add(Blocks.AIR, 0,52,-33);
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
