package mc.duzo.beyondtheend.common.register;

import com.TBK.beyond_the_end.common.registry.BkCommonRegistry;
import com.google.common.collect.ImmutableSet;
import mc.duzo.beyondtheend.EndersJourney;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BkPoi {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(ForgeRegistries.POI_TYPES, EndersJourney.MODID);

    public static final RegistryObject<PoiType> PORTAL = POI.register("portal", () -> new PoiType(getBlockStates(), 0, 1));

    public static final RegistryObject<PoiType> THE_NEW_END_PORTAL = POI.register("the_new_end_portal", () -> new PoiType(getBlockStates(BKBlocks.THE_NEW_END_PORTAL.get()), 0, 1));

    private static Set<BlockState> getBlockStates(Block block) {

        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
    private static Set<BlockState> getBlockStates() {
        List<BlockState> states = new ArrayList<>(BKBlocks.PORTAL.get().getStateDefinition().getPossibleStates());
        states.addAll(BKBlocks.PORTAL_NETHER.get().getStateDefinition().getPossibleStates());
        states.addAll(BKBlocks.PORTAL_OVERWORLD.get().getStateDefinition().getPossibleStates());
        return ImmutableSet.copyOf(states);
    }
}
