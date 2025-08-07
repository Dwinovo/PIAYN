package com.dwinovo.piayn.command;

import java.util.List;
import java.util.Map;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.packet.ServerModelDataPacket;
import com.dwinovo.piayn.server.resource.ServerModelDataInitializer;
import com.dwinovo.piayn.server.resource.ServerModelDataLoader;
import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * PIAYN模组重载指令
 */
public class ModelDataReloadCommand {
    
    /**
     * 注册重载指令
     * 
     * @param dispatcher 指令调度器
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("piayn")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .then(Commands.literal("reload")
                    .executes(ModelDataReloadCommand::executeReload)
                )
        );
    }
    
    /**
     * 执行重载指令
     * 
     * @param context 指令上下文
     * @return 指令执行结果
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 获取所有在线玩家
            List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            // 清空服务器模型数据
            ServerModelDataLoader.clearModelData();
            ServerModelDataInitializer.initializeModelDirectory();
            ServerModelDataLoader.ModelDataLoadEnterPoint();
            
            // 遍历所有在线玩家
            for (ServerPlayer player : players) {
                Map<String, ServerModelData> modelDataMap = ServerModelDataLoader.getModelDataMap();
                for (ServerModelData modelData : modelDataMap.values()) {
                    ServerModelDataPacket packet = ServerModelDataPacket.fromServerModelData(modelData);
                    PacketDistributor.sendToPlayer(player, packet);
                }
            }
            Component successMessage = Component.literal("§a[PIAYN] 重载成功");
            source.sendSuccess(() -> successMessage, false);
            return 1; // 成功执行
            
        } catch (Exception e) {
            Component errorMessage = Component.literal("§c[PIAYN] 重载失败: " + e.getMessage());
            source.sendFailure(errorMessage);
            
            PIAYN.LOGGER.error("Failed to execute PIAYN reload command", e);
            return 0; // 执行失败
        }
    }
}
