package mc.duzo.beyondtheend.common.register;

import mc.duzo.beyondtheend.EndersJourney;
import mc.duzo.beyondtheend.common.block_entity.ColumnBlockEntity;
import mc.duzo.beyondtheend.common.block_entity.TheNewEndBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BKBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EndersJourney.MODID);

    public static final RegistryObject<BlockEntityType<ColumnBlockEntity>> COLUMN_ENTITY =
            BLOCK_ENTITIES.register("column_entity", () ->
                    BlockEntityType.Builder.of(ColumnBlockEntity::new,
                            BKBlocks.COLUMN.get()).build(null));

    public static final RegistryObject<BlockEntityType<TheNewEndBlockEntity>> THE_NEW_END_PORTAL_ENTITY =
            BLOCK_ENTITIES.register("the_new_end_portal_entity", () ->
                    BlockEntityType.Builder.of(TheNewEndBlockEntity::new,
                            BKBlocks.THE_NEW_END_PORTAL.get()).build(null));


}
