package com.dwinovo.piayn.event.schematic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

import java.io.IOException;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.renderer.schematic.SchematicRenderer;
import com.dwinovo.piayn.item.SchematicPaperItem;
import com.dwinovo.piayn.item.SchematicPenItem;
import com.dwinovo.piayn.schematic.nbt.NbtStructureIO;
import com.dwinovo.piayn.utils.RaycastHelper;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@EventBusSubscriber(modid = PIAYN.MOD_ID,value = Dist.CLIENT)
public class SchematicPaperClientEvent {
    public static StructureTemplate structureTemplate;
    public static SchematicLevel schematicLevel;
    public static StructurePlaceSettings placementSettings;
    public static SchematicRenderer renderer = new SchematicRenderer();
    
    public static boolean isInit = false;


    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof SchematicPaperItem)) return;
        
        if (!isInit){
            init(level);
        }

        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        
        // 正确的SuperRenderTypeBuffer获取方式
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        
        // 调用渲染
        renderer.render(ms, buffer);
        
        ms.popPose();
        

    }

    public static void init(Level level){
        try {
            structureTemplate = NbtStructureIO.loadNbtToStructureTemplate(level, "test");
        } catch (IOException e) {
            e.printStackTrace();
        }
        schematicLevel = new SchematicLevel(level);
        placementSettings = new StructurePlaceSettings();
        structureTemplate.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, placementSettings, level.getRandom(), Block.UPDATE_CLIENTS);
        for (BlockEntity blockEntity : schematicLevel.getBlockEntities()) {
            blockEntity.setLevel(schematicLevel);
        }
        renderer.display(schematicLevel);
        isInit = true;
    }
}
