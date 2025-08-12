package com.dwinovo.piayn.event.schematic;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

import com.dwinovo.piayn.PIAYN;

import com.dwinovo.piayn.lib.catnip.outliner.Outliner;
import com.dwinovo.piayn.lib.catnip.render.BindableTexture;
import com.dwinovo.piayn.schematic.nbt.NbtStructureIO;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端事件：在渲染阶段通过 showCluster 展示 5 个草方块位置的“方块簇轮廓”（仅边框）。
 * 渲染与 tick 由 {@link SchematicPenClientEvent} 统一驱动。
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID, value = Dist.CLIENT)
public class SchematicPaperClientEvent {
    private static StructureTemplate template;

    private static boolean init = false;

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
        
        

        
    }
    public static void init(Level level) {
        try {
            template = NbtStructureIO.loadNbtToStructureTemplate(level, "test");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
