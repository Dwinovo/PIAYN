package com.dwinovo.piayn.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.network.packet.ModelSwitchPacket;

/**
 * PIAYN模组网络通信管理类
 * 负责注册和管理所有网络包
 */
public class PIAYNNetworking {
    
    /**
     * 注册网络包处理器
     * @param event 注册事件
     */
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PIAYN.MOD_ID);
        
        // 注册模型切换包
        registrar.playToServer(
            ModelSwitchPacket.TYPE,
            ModelSwitchPacket.STREAM_CODEC,
            ModelSwitchPacket::handleServer
        );
        
        PIAYN.LOGGER.info("PIAYN network packets registered successfully");
    }
}
