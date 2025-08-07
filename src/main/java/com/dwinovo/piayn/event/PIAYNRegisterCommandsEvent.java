package com.dwinovo.piayn.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.command.ModelDataReloadCommand;


@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PIAYNRegisterCommandsEvent {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModelDataReloadCommand.register(event.getDispatcher());
    }
}
