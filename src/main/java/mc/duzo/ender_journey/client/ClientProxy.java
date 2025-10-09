package mc.duzo.ender_journey.client;

import mc.duzo.ender_journey.CommonProxy;
import mc.duzo.ender_journey.client.renderer.ColumnRenderer;
import mc.duzo.ender_journey.common.register.BKBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    public void init(){

        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        BlockEntityRenderers.register(BKBlockEntity.COLUMN_ENTITY.get(), ColumnRenderer::new);
        BlockEntityRenderers.register(BKBlockEntity.THE_NEW_END_PORTAL_ENTITY.get(), TheEndPortalRenderer::new);

    }


}
