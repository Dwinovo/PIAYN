package com.dwinovo.piayn;

import org.slf4j.Logger;

import com.dwinovo.piayn.init.InitEntity;
import com.dwinovo.piayn.init.InitMenuTypes;
import com.dwinovo.piayn.network.PIAYNNetworking;
import com.dwinovo.piayn.network.packet.ServerModelDataPacket;
import com.dwinovo.piayn.server.resource.ServerModelDataInitializer;
import com.dwinovo.piayn.server.resource.ServerModelDataLoader;
import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModContainer;


@Mod(PIAYN.MOD_ID)
@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PIAYN {

    public static final String MOD_ID = "piayn";

    public static final Logger LOGGER = LogUtils.getLogger();

    public PIAYN(IEventBus modEventBus, ModContainer modContainer) {
        initRegister(modEventBus);
        
        // 注册网络包处理器
        modEventBus.addListener(PIAYNNetworking::registerPackets);
    }
    private static void initRegister(IEventBus modEventBus) {
        InitEntity.ENTITY_TYPES.register(modEventBus);
        InitMenuTypes.MENU_TYPES.register(modEventBus);
    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartedEvent event) {
        ServerModelDataInitializer.initializeModelDirectory();
        ServerModelDataLoader.ModelDataLoadEnterPoint();
        
    }
    
    /**
     * 玩家加入服务器时发送所有模型数据
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取服务端加载的所有模型数据
            var modelDataMap = ServerModelDataLoader.getModelDataMap();
            
            // 发送每个模型数据到客户端
            for (ServerModelData modelData : modelDataMap.values()) {
                ServerModelDataPacket packet = ServerModelDataPacket.fromServerModelData(modelData);
                PacketDistributor.sendToPlayer(player, packet);
            }
            
            LOGGER.info("Sent {} model data packets to player: {}", 
                modelDataMap.size(), player.getName().getString());
        }
    }
    


    
}
