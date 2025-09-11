package mc.duzo.beyondtheend.common.register;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.common.blocks.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BKBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EndersJourney.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EndersJourney.MODID);

    public static final RegistryObject<Block> COLUMN = registerBlock("column",
            () -> new ColumnBlock(BlockBehaviour.Properties.copy(Blocks.END_PORTAL))
    );

    public static final RegistryObject<Block> THE_NEW_END_PORTAL = registerBlock("the_new_end_portal",
            () -> new TheNewEndPortalBlock(BlockBehaviour.Properties.copy(Blocks.END_PORTAL))
    );
    public static final RegistryObject<Block> PORTAL = registerBlock("portal",
            () -> new PortalBlock(BlockBehaviour.Properties.copy(Blocks.END_PORTAL))
    );
    public static final RegistryObject<Block> PORTAL_OVERWORLD = registerBlock("portal_overworld",
            () -> new PortalOverWorldBlock(BlockBehaviour.Properties.copy(Blocks.END_PORTAL))
    );
    public static final RegistryObject<Block> PORTAL_NETHER = registerBlock("portal_nether",
            () -> new PortalNetherBlock(BlockBehaviour.Properties.copy(Blocks.END_PORTAL))
    );

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block){
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends  Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    }
}
