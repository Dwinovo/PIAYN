package com.dwinovo.piayn.event.schematic;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.renderer.catnip.render.DefaultSuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.schematic.SchematicPreviewRenderer;
import com.dwinovo.piayn.item.SchematicPaperItem;
import com.dwinovo.piayn.world.schematic.io.NbtStructureIO;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.io.IOException;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

/**
 * 客户端事件：在 AFTER_PARTICLES 阶段渲染简化的蓝图预览。
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID, value = Dist.CLIENT)
public class SchematicPaperClientEvent {
    private static StructureTemplate template;

    private static boolean init = false;
    private static SchematicLevel previewLevel;
    private static final SchematicPreviewRenderer renderer = new SchematicPreviewRenderer();

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

        if (!init) {
            init(level);
            init = true;
        }
        // 持有蓝图才显示
        if (!isHoldingSchematicPaper(player))
            return;

        if (template == null)
            return;

        // 命中位置作为 anchor（方块面外推一格），否则使用脚下上方
        BlockPos anchor = getTargetAnchor(mc, player);

        // 使用新的 SchematicLevel（防止积累），并将模板放置到其中
        previewLevel = new SchematicLevel(level);
        previewLevel.anchor = anchor;
        StructurePlaceSettings settings = new StructurePlaceSettings();
        RandomSource random = RandomSource.create();
        template.placeInWorld(previewLevel, anchor, anchor, settings, random, 2);

        // 渲染
        SchematicPaperClientEvent.renderPreview(event, previewLevel, anchor);
    }
    public static void init(Level level) {
        try {
            template = NbtStructureIO.loadNbtToStructureTemplate(level, "test");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    private static boolean isHoldingSchematicPaper(Player player) {
        ItemStack mh = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack oh = player.getItemInHand(InteractionHand.OFF_HAND);
        return mh.getItem() instanceof SchematicPaperItem || oh.getItem() instanceof SchematicPaperItem;
    }

    private static BlockPos getTargetAnchor(Minecraft mc, Player player) {
        HitResult hr = mc.hitResult;
        if (hr instanceof BlockHitResult bhr) {
            return bhr.getBlockPos().relative(bhr.getDirection());
        }
        return player.blockPosition().above();
    }

    private static void renderPreview(RenderLevelStageEvent event, SchematicLevel level, BlockPos anchor) {
        // 摄像机平移
        Vec3 cam = event.getCamera().getPosition();
        var ms = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        ms.pushPose();
        ms.translate(-cam.x, -cam.y, -cam.z);
        renderer.display(level);
        renderer.setAnchor(anchor);
        renderer.setActive(true);
        // advance chasing state once per frame, then render with partial ticks
        renderer.tick();
        // RenderLevelStageEvent#getPartialTick returns a DeltaTracker in 1.21; extract game-time partial tick
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        renderer.render(ms, buffers, pt);
        ms.popPose();
        buffers.draw();
    }
}
