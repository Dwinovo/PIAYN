package com.dwinovo.piayn.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;

/**
 * 模型切换网络包
 * 用于客户端向服务端发送宠物模型切换请求
 */
public record ModelSwitchPacket(int petEntityId, String targetModelId) implements CustomPacketPayload {
    
    public static final Type<ModelSwitchPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "model_switch")
    );
    
    public static final StreamCodec<FriendlyByteBuf, ModelSwitchPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ModelSwitchPacket::petEntityId,
            ByteBufCodecs.STRING_UTF8, ModelSwitchPacket::targetModelId,
            ModelSwitchPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * 服务端处理模型切换包
     * @param packet 收到的包数据
     * @param context 网络上下文
     */
    public static void handleServer(ModelSwitchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取发送包的玩家
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) {
                PIAYN.LOGGER.warn("Received ModelSwitchPacket but player is null");
                return;
            }
            
            // 在服务端世界中查找宠物实体
            Entity entity = player.serverLevel().getEntity(packet.petEntityId());
            if (!(entity instanceof PetEntity petEntity)) {
                PIAYN.LOGGER.warn("Entity with ID {} is not a PetEntity or does not exist", packet.petEntityId());
                return;
            }
            
            
            // 验证目标模型ID是否有效
            String targetModelId = packet.targetModelId();
            if (targetModelId == null || targetModelId.trim().isEmpty()) {
                PIAYN.LOGGER.warn("Invalid target model ID: {}", targetModelId);
                return;
            }
            
            // 在服务端切换宠物模型
            String oldModelId = petEntity.getModelID();
            petEntity.setModelID(targetModelId);
            
            PIAYN.LOGGER.info("Player {} switched pet model from '{}' to '{}' for pet ID {}", 
                player.getName().getString(), oldModelId, targetModelId, packet.petEntityId());
            
            // 服务端的模型切换会自动同步到所有客户端
            // 因为PetEntity的数据同步机制会处理这个
        });
    }
}
