package com.dwinovo.piayn;

import com.dwinovo.piayn.client.model.PetEntityModel;
import com.dwinovo.piayn.client.renderer.PetEnityRenderer;
import com.dwinovo.piayn.init.InitEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = PIAYN.MOD_ID, dist = Dist.CLIENT)

@EventBusSubscriber(modid = PIAYN.MOD_ID, value = Dist.CLIENT)
public class PIAYNClient {
    
    public PIAYNClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
	public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
		/*
		 * 注册渲染器
		 */
		event.registerEntityRenderer(InitEntity.PET.get(), context -> new PetEnityRenderer(context, new PetEntityModel()));
	}
}
