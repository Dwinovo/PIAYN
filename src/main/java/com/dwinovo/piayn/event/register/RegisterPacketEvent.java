package com.dwinovo.piayn.event.register;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.packet.ModelSwitchPacket;
import com.dwinovo.piayn.packet.ServerModelDataPacket;
import com.dwinovo.piayn.packet.SchematicSelectPacket;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class RegisterPacketEvent {
    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PIAYN.MOD_ID);
        
        // 注册模型切换包
        registrar.playToServer(
            ModelSwitchPacket.TYPE,
            ModelSwitchPacket.STREAM_CODEC,
            ModelSwitchPacket::handleServer
        );
        registrar.playToClient(
            ServerModelDataPacket.TYPE,
            ServerModelDataPacket.STREAM_CODEC,
            ServerModelDataPacket::handleClient
        );
        // 注册蓝图选择包（客户端 -> 服务端）
        registrar.playToServer(
            SchematicSelectPacket.TYPE,
            SchematicSelectPacket.STREAM_CODEC,
            SchematicSelectPacket::handleServer
        );
    
    }
}
