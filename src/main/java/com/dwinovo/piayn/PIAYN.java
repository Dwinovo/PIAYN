package com.dwinovo.piayn;

import org.slf4j.Logger;

import com.dwinovo.piayn.init.InitEntity;
import com.dwinovo.piayn.init.InitMenuTypes;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;


@Mod(PIAYN.MOD_ID)
public class PIAYN {

    public static final String MOD_ID = "piayn";

    public static final Logger LOGGER = LogUtils.getLogger();

    public PIAYN(IEventBus modEventBus, ModContainer modContainer) {
        initRegister(modEventBus);
        
        
    }
    private static void initRegister(IEventBus modEventBus) {
        InitEntity.ENTITY_TYPES.register(modEventBus);
        InitMenuTypes.MENU_TYPES.register(modEventBus);
    }


    
}
