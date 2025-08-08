package com.dwinovo.piayn.client.renderer.schem;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import com.dwinovo.piayn.item.CreateStickItem;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = "piayn", value = Dist.CLIENT)
public class SchemRegionSelectRenderer {
    
    private static final float[] RED_COLOR = {1.0f, 0.2f, 0.2f, 0.6f}; // 红色透明
    private static final float[] GREEN_COLOR = {0.2f, 1.0f, 0.2f, 0.6f}; // 绿色透明
    private static final float LINE_WIDTH = 2.0f;
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) {
            return;
        }
        
        // 检查玩家是否持有CreateStickItem
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();
        
        CompoundTag tag = null;
        if (mainHandItem.getItem() instanceof CreateStickItem) {
            CustomData customData = mainHandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            tag = customData.copyTag();
        } else if (offHandItem.getItem() instanceof CreateStickItem) {
            CustomData customData = offHandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            tag = customData.copyTag();
        }
        
        if (tag == null) {
            return;
        }
        
        // 检查是否在圈地模式
        int currentMode = tag.getInt("mode");
        if (currentMode != 0) { // 0 = MODE_SELECT
            return;
        }
        
        // 获取选区点位
        BlockPos pos1 = null;
        BlockPos pos2 = null;
        
        if (tag.getBoolean("hasPos1")) {
            CompoundTag pos1Tag = tag.getCompound("pos1");
            pos1 = new BlockPos(pos1Tag.getInt("x"), pos1Tag.getInt("y"), pos1Tag.getInt("z"));
        }
        
        if (tag.getBoolean("hasPos2")) {
            CompoundTag pos2Tag = tag.getCompound("pos2");
            pos2 = new BlockPos(pos2Tag.getInt("x"), pos2Tag.getInt("y"), pos2Tag.getInt("z"));
        }
        
        // 渲染选区
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        Matrix4f matrix = poseStack.last().pose();
        
        if (pos1 != null) {
            renderCornerBox(matrix, pos1, GREEN_COLOR);
        }
        
        if (pos2 != null) {
            renderCornerBox(matrix, pos2, GREEN_COLOR);
        }
        
        if (pos1 != null && pos2 != null) {
            renderSelectionBox(matrix, pos1, pos2, RED_COLOR);
        }
        
        poseStack.popPose();
    }
    
    private static void renderCornerBox(Matrix4f matrix, BlockPos pos, float[] color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(LINE_WIDTH);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        
        // 渲染一个小的立方体边框来标记角点
        float size = 0.1f;
        
        // 底面
        buffer.addVertex(matrix, x - size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x - size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        // 顶面
        buffer.addVertex(matrix, x - size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x - size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        // 竖直边
        buffer.addVertex(matrix, x - size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y - size, z - size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z - size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x + 1 + size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x + 1 + size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, x - size, y - size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x - size, y + 1 + size, z + 1 + size).setColor(color[0], color[1], color[2], color[3]);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        RenderSystem.disableBlend();
    }
    
    private static void renderSelectionBox(Matrix4f matrix, BlockPos pos1, BlockPos pos2, float[] color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(LINE_WIDTH);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        // 计算选区边界
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        
        // 底面边框
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        // 顶面边框
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        // 竖直边框
        buffer.addVertex(matrix, minX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(color[0], color[1], color[2], color[3]);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        
        RenderSystem.disableBlend();
    }
}
