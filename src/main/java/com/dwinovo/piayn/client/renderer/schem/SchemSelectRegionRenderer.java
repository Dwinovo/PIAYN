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
public class SchemSelectRegionRenderer {
    
    private static final float[] RED_COLOR = {1.0f, 0.2f, 0.2f, 0.6f}; // 红色透明
    private static final float[] GREEN_COLOR = {0.2f, 1.0f, 0.2f, 0.6f}; // 绿色透明
    private static final float LINE_THICKNESS = 0.01f; // 线条厚度（使用四边形模拟）
    
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
        
        int currentMode = tag.getInt("mode");
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        Matrix4f matrix = poseStack.last().pose();

        if (currentMode == 0) { // MODE_SELECT
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

            if (pos1 != null) {
                renderCornerBox(matrix, pos1, GREEN_COLOR);
            }
            if (pos2 != null) {
                renderCornerBox(matrix, pos2, GREEN_COLOR);
            }
            if (pos1 != null && pos2 != null) {
                renderSelectionBox(matrix, pos1, pos2, RED_COLOR);
            }
        }
        
        poseStack.popPose();
    }
    
    /**
     * 设置渲染状态并创建缓冲区（线条模式）
     * @return BufferBuilder 配置好的缓冲区
     */
    private static BufferBuilder setupLineRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull(); // 禁用面剔除以确保四边形可见
        
        Tesselator tesselator = Tesselator.getInstance();
        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }
    
    /**
     * 完成渲染并清理状态
     * @param buffer 要绘制的缓冲区
     */
    private static void finishRender(BufferBuilder buffer) {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull(); // 重新启用面剔除
        RenderSystem.disableBlend();
    }
    
    /**
     * 添加粗线条到缓冲区（使用多个四边形模拟圆柱体）
     * @param buffer 缓冲区
     * @param matrix 变换矩阵
     * @param x1 起点X坐标
     * @param y1 起点Y坐标
     * @param z1 起点Z坐标
     * @param x2 终点X坐标
     * @param y2 终点Y坐标
     * @param z2 终点Z坐标
     * @param color 颜色数组
     */
    private static void addThickLine(BufferBuilder buffer, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float[] color) {
        // 计算线条方向向量
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        
        // 计算线条长度
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001f) return; // 避免除零
        
        // 归一化方向向量
        dx /= length;
        dy /= length;
        dz /= length;
        
        // 计算两个互相垂直的向量来创建线条的"圆形"截面
        // 使用向量叉积来找到垂直向量
        float perpX1, perpY1, perpZ1;
        float perpX2, perpY2, perpZ2;
        
        // 选择一个不与线条方向平行的参考向量
        if (Math.abs(dx) < 0.9f) {
            // 使用X轴作为参考向量
            perpX1 = 0;
            perpY1 = dz;
            perpZ1 = -dy;
        } else {
            // 使用Y轴作为参考向量
            perpX1 = -dz;
            perpY1 = 0;
            perpZ1 = dx;
        }
        
        // 归一化第一个垂直向量
        float perpLen1 = (float) Math.sqrt(perpX1 * perpX1 + perpY1 * perpY1 + perpZ1 * perpZ1);
        if (perpLen1 > 0.001f) {
            perpX1 /= perpLen1;
            perpY1 /= perpLen1;
            perpZ1 /= perpLen1;
        }
        
        // 计算第二个垂直向量（与第一个垂直向量和线条方向都垂直）
        perpX2 = dy * perpZ1 - dz * perpY1;
        perpY2 = dz * perpX1 - dx * perpZ1;
        perpZ2 = dx * perpY1 - dy * perpX1;
        
        // 缩放垂直向量到所需厚度
        perpX1 *= LINE_THICKNESS;
        perpY1 *= LINE_THICKNESS;
        perpZ1 *= LINE_THICKNESS;
        perpX2 *= LINE_THICKNESS;
        perpY2 *= LINE_THICKNESS;
        perpZ2 *= LINE_THICKNESS;
        
        // 创建多个四边形来模拟圆柱体（4个面，形成近似圆形截面）
        // 第一个四边形：+perpX1 方向
        buffer.addVertex(matrix, x1 + perpX1, y1 + perpY1, z1 + perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x1 + perpX2, y1 + perpY2, z1 + perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 + perpX2, y2 + perpY2, z2 + perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 + perpX1, y2 + perpY1, z2 + perpZ1).setColor(color[0], color[1], color[2], color[3]);
        
        // 第二个四边形：+perpX2 方向
        buffer.addVertex(matrix, x1 + perpX2, y1 + perpY2, z1 + perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x1 - perpX1, y1 - perpY1, z1 - perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 - perpX1, y2 - perpY1, z2 - perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 + perpX2, y2 + perpY2, z2 + perpZ2).setColor(color[0], color[1], color[2], color[3]);
        
        // 第三个四边形：-perpX1 方向
        buffer.addVertex(matrix, x1 - perpX1, y1 - perpY1, z1 - perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x1 - perpX2, y1 - perpY2, z1 - perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 - perpX2, y2 - perpY2, z2 - perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 - perpX1, y2 - perpY1, z2 - perpZ1).setColor(color[0], color[1], color[2], color[3]);
        
        // 第四个四边形：-perpX2 方向
        buffer.addVertex(matrix, x1 - perpX2, y1 - perpY2, z1 - perpZ2).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x1 + perpX1, y1 + perpY1, z1 + perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 + perpX1, y2 + perpY1, z2 + perpZ1).setColor(color[0], color[1], color[2], color[3]);
        buffer.addVertex(matrix, x2 - perpX2, y2 - perpY2, z2 - perpZ2).setColor(color[0], color[1], color[2], color[3]);
    }
    
    /**
     * 添加立方体边框粗线条到缓冲区
     * @param buffer 缓冲区
     * @param matrix 变换矩阵
     * @param minX 最小X坐标
     * @param minY 最小Y坐标
     * @param minZ 最小Z坐标
     * @param maxX 最大X坐标
     * @param maxY 最大Y坐标
     * @param maxZ 最大Z坐标
     * @param color 颜色数组
     */
    private static void addBoxThickLines(BufferBuilder buffer, Matrix4f matrix, 
                                        float minX, float minY, float minZ,
                                        float maxX, float maxY, float maxZ, 
                                        float[] color) {
        // 底面边框
        addThickLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        addThickLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        addThickLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        
        // 顶面边框
        addThickLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        addThickLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        addThickLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        
        // 竖直边框
        addThickLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        addThickLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
    }
    
    private static void renderCornerBox(Matrix4f matrix, BlockPos pos, float[] color) {
        BufferBuilder buffer = setupLineRenderState();
        
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        
        // 渲染一个小的立方体边框来标记角点
        float size = 0.1f;
        
        addBoxThickLines(buffer, matrix, 
                        x - size, y - size, z - size,
                        x + 1 + size, y + 1 + size, z + 1 + size, 
                        color);
        
        finishRender(buffer);
    }
    
    private static void renderSelectionBox(Matrix4f matrix, BlockPos pos1, BlockPos pos2, float[] color) {
        BufferBuilder buffer = setupLineRenderState();
        
        // 计算选区边界
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        
        addBoxThickLines(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, color);
        
        finishRender(buffer);
    }
}
