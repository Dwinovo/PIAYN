package com.dwinovo.piayn.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.command.ModelDataReloadCommand;


@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class RegisterCommandsEvent {

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        ModelDataReloadCommand.register(event.getDispatcher());
    }
}
