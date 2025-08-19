package com.dwinovo.piayn.event.schematic;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.schematic.ClientSchematicManager;
import com.dwinovo.piayn.utils.ClientUtils;
import com.dwinovo.piayn.init.InitComponent;
import com.dwinovo.piayn.item.SchematicPaperItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端事件：在世界渲染阶段绘制蓝图预览（不改变世界）。
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID, value = Dist.CLIENT)
public class SchematicPaperClientEvent {
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 选择在半透明之后阶段绘制，避免与世界方块排序冲突
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null)
            return;

        // 获取持有的蓝图纸物品和文件名（未持有或未写入文件名则不渲染）
        ItemStack schematicPaper = isHoldingSchematicPaper(player);
        if (schematicPaper.isEmpty())
            return;

        // 从数据组件获取文件名
        String fileName = schematicPaper.get(InitComponent.SCHEMATIC_NAME.get());
        if (fileName == null || fileName.isEmpty())
            return;

        // 确保 Manager 侧按需加载并创建/复用当前 SchematicLevel
        ClientSchematicManager.getInstance().display(level, fileName);

        BlockPos anchor = Optional.ofNullable(
            schematicPaper.get(InitComponent.SCHEMATIC_ANCHOR.get())
        ).orElse(ClientUtils.getLookingAt(mc, player, 10));
        
        // 渲染：完全委托给管理器（内部处理 null 检查与渲染器调用）
        ClientSchematicManager.getInstance().renderPreview(event, anchor);
    }

    /**
     * 获取玩家持有的蓝图纸物品
     * @param player 玩家
     * @return 蓝图纸物品，如果没有持有则返回空ItemStack
     */
    /**
     * 从主副手中找到蓝图纸物品。
     */
    private static ItemStack isHoldingSchematicPaper(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        if (mainHand.getItem() instanceof SchematicPaperItem) {
            return mainHand;
        }
        if (offHand.getItem() instanceof SchematicPaperItem) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }
}

