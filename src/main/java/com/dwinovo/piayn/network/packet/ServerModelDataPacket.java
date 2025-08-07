package com.dwinovo.piayn.network.packet;

import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import com.dwinovo.piayn.client.resource.ClientModelDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端模型数据网络包
 * 用于将ServerModelData从服务端发送到客户端
 */
public record ServerModelDataPacket(
    String modelName,
    String modelID,
    byte[] model,
    byte[] animation,
    byte[] texture
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<ServerModelDataPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("piayn", "server_model_data"));
    
    public static final StreamCodec<FriendlyByteBuf, ServerModelDataPacket> STREAM_CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerModelDataPacket::modelName,
            ByteBufCodecs.STRING_UTF8, ServerModelDataPacket::modelID,
            ByteBufCodecs.BYTE_ARRAY, ServerModelDataPacket::model,
            ByteBufCodecs.BYTE_ARRAY, ServerModelDataPacket::animation,
            ByteBufCodecs.BYTE_ARRAY, ServerModelDataPacket::texture,
            ServerModelDataPacket::new
        );
    
    /**
     * 从ServerModelData创建网络包
     */
    public static ServerModelDataPacket fromServerModelData(ServerModelData data) {
        return new ServerModelDataPacket(
            data.getModelName(),
            data.getModelID(),
            data.getModel(),
            data.getAnimation(),
            data.getTexture()
        );
    }
    
    /**
     * 转换为ServerModelData
     */
    public ServerModelData toServerModelData() {
        return new ServerModelData(modelName, modelID, model, animation, texture);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * 客户端处理服务端模型数据包
     * @param packet 接收到的数据包
     * @param context 网络上下文
     */
    public static void handleClient(ServerModelDataPacket packet, 
            net.neoforged.neoforge.network.handling.IPayloadContext context) {
        
        context.enqueueWork(() -> {
            // 将接收到的网络包转换为ServerModelData
            ServerModelData serverModelData = packet.toServerModelData();
            
            // 使用ClientModelDataManager处理模型数据
            boolean success = ClientModelDataManager.processServerModelData(serverModelData);
            
        });
    }
}
