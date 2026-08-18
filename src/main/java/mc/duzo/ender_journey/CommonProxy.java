package mc.duzo.ender_journey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import tictim.paraglider.ParagliderMod;

import java.util.ArrayList;
import java.util.List;

public class CommonProxy {
    public static final List<PlacedBlock> BLOCKS = new ArrayList<>();
    public static boolean blocksInitialized = false;

    public void init(){

    }
    public static void initBlocks(){
        if (blocksInitialized) return;
        blocksInitialized = true;
        EndersJourney.LOGGER.info("[ENDERJOURNEY-DEBUG] initBlocks() running this session.");

        Events.queue.add(List.of(
                new Vec3i(1,0,0),
                new Vec3i(-1,0,0),
                new Vec3i(0,0,1),
                new Vec3i(0,0,-1)
        ));

        Events.queue.add(List.of(
                new Vec3i(1,0,1),
                new Vec3i(-1,0,1),
                new Vec3i(1,0,-1),
                new Vec3i(-1,0,-1)
        ));

        Events.queue.add(List.of(
                new Vec3i(2,0,0),
                new Vec3i(-2,0,0),
                new Vec3i(0,0,2),
                new Vec3i(0,0,-2)
        ));

        Events.queue.add(List.of(
                new Vec3i(2,0,-1),
                new Vec3i(-2,0,1),
                new Vec3i(2,0,1),
                new Vec3i(-2,0,-1)
        ));

        Events.queue.add(List.of(
                new Vec3i(1,0,-2),
                new Vec3i(-1,0,2),
                new Vec3i(1,0,2),
                new Vec3i(-1,0,-2)
        ));

        Events.queue.add(List.of(
                new Vec3i(2,0,2),
                new Vec3i(-2,0,2),
                new Vec3i(2,0,-2),
                new Vec3i(-2,0,-2)
        ));

        Events.queue.add(List.of(
                new Vec3i(3,0,0),
                new Vec3i(-3,0,0),
                new Vec3i(0,0,3),
                new Vec3i(0,0,-3)
        ));

        Events.queue.add(List.of(
                new Vec3i(3,0,-1),
                new Vec3i(-3,0,1),
                new Vec3i(3,0,1),
                new Vec3i(-3,0,-1)
        ));

        Events.queue.add(List.of(
                new Vec3i(1,0,3),
                new Vec3i(-1,0,3),
                new Vec3i(1,0,-3),
                new Vec3i(-1,0,-3)
        ));

        Events.queue.add(List.of(
                new Vec3i(3,0,-2),
                new Vec3i(-3,0,2),
                new Vec3i(3,0,2),
                new Vec3i(-3,0,-2)
        ));

        Events.queue.add(List.of(
                new Vec3i(2,0,3),
                new Vec3i(-2,0,3),
                new Vec3i(2,0,-3),
                new Vec3i(-2,0,-3)
        ));

        Events.queue.add(List.of(
                new Vec3i(4,0,0),
                new Vec3i(-4,0,0),
                new Vec3i(0,0,4),
                new Vec3i(0,0,-4)
        ));

        Events.queue.add(List.of(
                new Vec3i(4,0,-1),
                new Vec3i(-4,0,1),
                new Vec3i(4,0,1),
                new Vec3i(-4,0,-1)
        ));

        Events.queue.add(List.of(
                new Vec3i(1,0,4),
                new Vec3i(-1,0,4),
                new Vec3i(1,0,-4),
                new Vec3i(-1,0,-4)
        ));

        Events.queue.add(List.of(
                new Vec3i(3,0,3),
                new Vec3i(-3,0,3),
                new Vec3i(3,0,-3),
                new Vec3i(-3,0,-3)
        ));

        Events.queue.add(List.of(
                new Vec3i(4,0,-2),
                new Vec3i(-4,0,2),
                new Vec3i(4,0,2),
                new Vec3i(-4,0,-2)
        ));

        Events.queue.add(List.of(
                new Vec3i(2,0,4),
                new Vec3i(-2,0,4),
                new Vec3i(2,0,-4),
                new Vec3i(-2,0,-4)
        ));

        Events.queue.add(List.of(
                new Vec3i(5,0,0),
                new Vec3i(-5,0,0),
                new Vec3i(0,0,5),
                new Vec3i(0,0,-5)
        ));

        Events.queue.add(List.of(
                new Vec3i(4,0,3),
                new Vec3i(-4,0,3),
                new Vec3i(4,0,-3),
                new Vec3i(-4,0,-3)
        ));

        Events.queue.add(List.of(
                new Vec3i(3,0,4),
                new Vec3i(-3,0,4),
                new Vec3i(3,0,-4),
                new Vec3i(-3,0,-4)
        ));

        Events.queue.add(List.of(
                new Vec3i(5,0,-1),
                new Vec3i(-5,0,-1),
                new Vec3i(5,0,1),
                new Vec3i(-5,0,1)
        ));

        Events.queue.add(List.of(
                new Vec3i(1,0,-5),
                new Vec3i(-1,0,5),
                new Vec3i(1,0,5),
                new Vec3i(-1,0,-5)
        ));

        Events.queue.add(List.of(
                new Vec3i(5,0,2),
                new Vec3i(-5,0,2),
                new Vec3i(5,0,-2),
                new Vec3i(-5,0,-2)
        ));

        Events.queue.add(List.of(new Vec3i(2,0,-5), new Vec3i(-2,0,5), new Vec3i(2,0,5), new Vec3i(-2,0,-5)));

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
        for (int x = -8; x < 8 ; x ++){
            for(int z = -8; z < 8 ; z ++){
                add("beyondtheend:glowing_energy",5000+x,0,8+z);
            }
        }
        EndersJourney.LOGGER.info("[ENDERJOURNEY-DEBUG] initBlocks() done. BLOCKS.size()={}, queue.size()={}", BLOCKS.size(), Events.queue.size());
    }

    private static void add(String id, int x, int y, int z) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (block != null) BLOCKS.add(new PlacedBlock(block, new BlockPos(x,y,z), null));
    }

    public record PlacedBlock(Block block, BlockPos pos, Direction facing) {

    }
}
