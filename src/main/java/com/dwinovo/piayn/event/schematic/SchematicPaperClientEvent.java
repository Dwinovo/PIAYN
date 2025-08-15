package com.dwinovo.piayn.event.schematic;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.renderer.catnip.render.DefaultSuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.schematic.SchematicPreviewRenderer;
import com.dwinovo.piayn.client.resource.schematic.ClientSchematicManager;
import com.dwinovo.piayn.init.InitComponent;
import com.dwinovo.piayn.item.SchematicPaperItem;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 客户端事件：在世界渲染阶段绘制蓝图预览（不改变世界）。
 *
 * 渲染时机（NeoForge 1.21.1）：
 * - 订阅 {@link RenderLevelStageEvent}，并选择 {@code Stage.AFTER_PARTICLES} 阶段绘制，
 *   避免与世界方块渲染排序和透明度发生冲突。
 *
 * 数据来源：
 * - 文件名从玩家手持的蓝图纸物品数据组件 {@code InitComponent.SCHEMATIC_NAME} 中读取；
 * - 蓝图数据从 {@link ClientSchematicManager#getCurrentLevel()} 获取（已由选择界面加载）。
 *
 * 渲染要点：
 * - 使用事件提供的 {@code PoseStack} 与相机信息进行相机平移（-cameraPos）；
 * - {@link RenderLevelStageEvent#getPartialTick()} 返回 {@code DeltaTracker}，
 *   通过 {@code getGameTimeDeltaPartialTick(false)} 取得 partial tick；
 * - 锚点（anchor）选取：若视线命中方块，则取命中面外推一格，否则取玩家脚下上方一格。
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID, value = Dist.CLIENT)
public class SchematicPaperClientEvent {
    private static final ClientSchematicManager schematicManager = ClientSchematicManager.getInstance();

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
        ItemStack schematicPaper = getHeldSchematicPaper(player);
        if (schematicPaper.isEmpty())
            return;

        // 从数据组件获取文件名
        String fileName = schematicPaper.get(InitComponent.SCHEMATIC_NAME.get());
        if (fileName == null || fileName.isEmpty())
            return;

        // 从ClientSchematicManager获取当前SchematicLevel（可能为null：尚未选择/已清空）
        SchematicLevel schematicLevel = schematicManager.getCurrentLevel();
        if (schematicLevel == null)
            return;

        // 命中位置作为 anchor（方块面外推一格），否则使用脚下上方
        BlockPos anchor = getTargetAnchor(mc, player);

        // 渲染
        renderPreview(event, schematicLevel, anchor);
    }

    /**
     * 获取玩家持有的蓝图纸物品
     * @param player 玩家
     * @return 蓝图纸物品，如果没有持有则返回空ItemStack
     */
    /**
     * 从主副手中找到蓝图纸物品。
     */
    private static ItemStack getHeldSchematicPaper(Player player) {
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

    /**
     * 计算锚点位置：
     * - 若鼠标命中方块，则返回命中面外推一格；
     * - 否则返回玩家脚下方块的上一格。
     */
    private static BlockPos getTargetAnchor(Minecraft mc, Player player) {
        HitResult hr = mc.hitResult;
        if (hr instanceof BlockHitResult bhr) {
            return bhr.getBlockPos().relative(bhr.getDirection());
        }
        return player.blockPosition().above();
    }

    /**
     * 渲染蓝图预览
     * @param event 渲染事件
     * @param level SchematicLevel实例
     * @param anchor 锚点位置
     */
    /**
     * 执行实际的蓝图预览渲染：
     * - 将相机位置平移入矩阵；
     * - 由渲染器在 render 内部推进一次追踪状态；
     * - 从 {@link RenderLevelStageEvent} 的 {@code DeltaTracker} 读取 partial tick；
     * - 使用自定义缓冲 {@link SuperRenderTypeBuffer} 绘制并 flush。
     */
    private static void renderPreview(RenderLevelStageEvent event, SchematicLevel level, BlockPos anchor) {
        // 摄像机平移
        Vec3 cam = event.getCamera().getPosition();
        var ms = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        ms.pushPose();
        ms.translate(-cam.x, -cam.y, -cam.z);
        SchematicPreviewRenderer renderer = ClientSchematicManager.getInstance().getRenderer();
        // 1.21 起 partialTick 是 DeltaTracker：提取游戏时间的插值分量
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        renderer.render(ms, buffers, pt, level, anchor);
        ms.popPose();
        buffers.draw();
    }
}

