package com.dwinovo.piayn.packet;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.init.InitComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端选择蓝图后发送给服务端，由服务端在对应手持物品上写入 SCHEMATIC_NAME 数据组件。
 */
public record SchematicSelectPacket(String fileName, InteractionHand hand) implements CustomPacketPayload {

    public static final Type<SchematicSelectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "schematic_select")
    );

    public static final StreamCodec<FriendlyByteBuf, SchematicSelectPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SchematicSelectPacket::fileName,
                    ByteBufCodecs.VAR_INT, p -> p.hand().ordinal(),
                    (name, handIdx) -> new SchematicSelectPacket(name, InteractionHand.values()[handIdx])
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(SchematicSelectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(packet.hand());
            if (!stack.isEmpty()) {
                stack.set(InitComponent.SCHEMATIC_NAME.get(), packet.fileName());
            }
        });
    }
}
