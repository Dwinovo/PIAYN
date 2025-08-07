package com.dwinovo.piayn.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.server.resource.ServerModelDataInitializer;
import com.dwinovo.piayn.server.resource.ServerModelDataLoader;

@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PIAYNServerStartedEvent {
    @SubscribeEvent
    public static void onServerStarting(ServerStartedEvent event) {
        ServerModelDataInitializer.initializeModelDirectory();
        ServerModelDataLoader.ModelDataLoadEnterPoint();
    }
}
