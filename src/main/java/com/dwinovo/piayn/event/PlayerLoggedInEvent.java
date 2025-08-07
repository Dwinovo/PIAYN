package com.dwinovo.piayn.event;

import com.dwinovo.piayn.packet.ServerModelDataPacket;
import com.dwinovo.piayn.server.resource.ServerModelDataLoader;
import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

import com.dwinovo.piayn.PIAYN;

@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PlayerLoggedInEvent {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Map<String, ServerModelData> modelDataMap = ServerModelDataLoader.getModelDataMap();
            for (ServerModelData modelData : modelDataMap.values()) {
                ServerModelDataPacket packet = ServerModelDataPacket.fromServerModelData(modelData);
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }
}
