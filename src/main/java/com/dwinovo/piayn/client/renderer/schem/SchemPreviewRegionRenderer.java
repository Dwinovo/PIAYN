package com.dwinovo.piayn.client.renderer.schem;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
 
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
import org.joml.Matrix4f;

import com.dwinovo.piayn.item.CreateStickItem;
import com.dwinovo.piayn.schem.SchemSerializer;
import com.dwinovo.piayn.schem.util.PaletteUtil;
import com.dwinovo.piayn.schem.util.VarIntUtil;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
 

/**
 * 专门负责“粘贴模式”的结构预渲染（体积外框 + 方块级半透明投影）
 */
@EventBusSubscriber(modid = "piayn", value = Dist.CLIENT)
public class SchemPreviewRegionRenderer {

    private static final float[] CYAN_COLOR = {0.2f, 0.9f, 1.0f, 0.5f}; // 青色透明
    private static final float LINE_THICKNESS = 0.01f; // 线条厚度（用于外框粗线）

    // 缓存：schem 尺寸与模型级相对坐标
    private static final Map<String, int[]> SCHEM_SIZE_CACHE = new HashMap<>();
    private static final Map<String, List<BlockEntry>> SCHEM_MODEL_CACHE = new HashMap<>();
    private static final int MAX_PREVIEW_BLOCKS = 5000; // 性能保护上限
    // 追踪客户端世界引用，用于跨世界切换时清空缓存
    private static ClientLevel LAST_CLIENT_LEVEL = null;
    // 轻量级指纹：用于检测同一世界中 .schem 文件内容是否更新（例如覆盖保存同名文件）
    private static final Map<String, Long> SCHEM_FINGERPRINT = new HashMap<>();
    private static long LAST_FINGERPRINT_CHECK_MS = 0L;
    private static String LAST_SELECTED_FILENAME = null;
    private static int LAST_MODE = -1;

    

    private static class BlockEntry {
        final int x, y, z;
        final BlockState state;
        BlockEntry(int x, int y, int z, BlockState s) { this.x=x; this.y=y; this.z=z; this.state=s; }
    }

    // 外部可调用：在保存/覆盖 .schem 后立即失效缓存（同名文件）
    public static void invalidateSchemCache(String filename) {
        if (filename == null || filename.isEmpty()) return;
        SCHEM_SIZE_CACHE.remove(filename);
        SCHEM_MODEL_CACHE.remove(filename);
        SCHEM_FINGERPRINT.remove(filename);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 若客户端世界已更换，清空 schem 预渲染缓存，避免跨世界复用旧数据
        if (mc.level != LAST_CLIENT_LEVEL) {
            SCHEM_SIZE_CACHE.clear();
            SCHEM_MODEL_CACHE.clear();
            SCHEM_FINGERPRINT.clear();
            LAST_CLIENT_LEVEL = mc.level;
        }

        // 仅当手持 CreateStickItem 且处于粘贴模式时才渲染
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        CompoundTag tag = null;
        if (main.getItem() instanceof CreateStickItem) {
            tag = main.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        } else if (off.getItem() instanceof CreateStickItem) {
            tag = off.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        }
        if (tag == null) return;
        int mode = tag.getInt("mode");
        if (mode != 1) { LAST_MODE = mode; return; } // MODE_PASTE
        if (!tag.getBoolean("hasPastePos")) return;

        String filename = tag.getString("selectedSchem");
        if (filename == null || filename.isEmpty()) { LAST_SELECTED_FILENAME = null; return; }

        // 文件名变更：立即使对应缓存失效，确保立刻加载新结构
        if (!filename.equals(LAST_SELECTED_FILENAME)) {
            // 不清空全部缓存，只移除关联项以保留其它文件缓存
            SCHEM_SIZE_CACHE.remove(filename);
            SCHEM_MODEL_CACHE.remove(filename);
            // 更新最后使用的文件名
            LAST_SELECTED_FILENAME = filename;
        }

        // 进入粘贴模式的瞬间：立即进行一次指纹校验，避免 1s 节流带来的可见延迟
        if (LAST_MODE != 1) {
            Long fp = computeSchemFingerprint(filename);
            if (fp != null) {
                Long old = SCHEM_FINGERPRINT.get(filename);
                if (old == null || !old.equals(fp)) {
                    SCHEM_SIZE_CACHE.remove(filename);
                    SCHEM_MODEL_CACHE.remove(filename);
                    SCHEM_FINGERPRINT.put(filename, fp);
                }
            }
        }
        LAST_MODE = mode;

        CompoundTag pp = tag.getCompound("pastePos");
        BlockPos pastePos = new BlockPos(pp.getInt("x"), pp.getInt("y"), pp.getInt("z"));

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        // 节流：每秒检查一次 .schem 文件是否被覆盖更新，若变更则清除对应缓存
        long now = System.currentTimeMillis();
        if (now - LAST_FINGERPRINT_CHECK_MS > 200L) {
            Long fp = computeSchemFingerprint(filename);
            if (fp != null) {
                Long old = SCHEM_FINGERPRINT.get(filename);
                if (old == null || !old.equals(fp)) {
                    SCHEM_SIZE_CACHE.remove(filename);
                    SCHEM_MODEL_CACHE.remove(filename);
                    SCHEM_FINGERPRINT.put(filename, fp);
                }
            }
            LAST_FINGERPRINT_CHECK_MS = now;
        }

        int[] size = getSchemSizeCached(filename);
        if (size != null) {
            renderPreviewBox(matrix, pastePos, size[0], size[1], size[2], CYAN_COLOR);
        }
        // 模型级投影
        List<BlockEntry> models = getModelPreviewDataCached(filename);
        if (models != null && !models.isEmpty()) {
            renderModelPreview(poseStack, pastePos, models);
        }

        poseStack.popPose();
    }

    // 统一渲染状态（QUADS + 透明）
    private static BufferBuilder setupLineRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void finishRender(BufferBuilder buffer) {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // 外框：粗线条立方体边框
    private static void addThickLine(BufferBuilder buffer, Matrix4f matrix,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2,
                                     float[] color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-3f) return;
        dx/=len; dy/=len; dz/=len;
        // 找到两个垂直向量（近似圆柱体）
        float ax = Math.abs(dx) < 0.9f ? 1f : 0f;
        float ay = ax == 1f ? 0f : 1f;
        float az = 0f;
        float px1 = ay*dz - az*dy, py1 = az*dx - ax*dz, pz1 = ax*dy - ay*dx;
        float inv = (float)(1f/Math.sqrt(px1*px1 + py1*py1 + pz1*pz1));
        px1*=inv; py1*=inv; pz1*=inv;
        float px2 = dy*pz1 - dz*py1, py2 = dz*px1 - dx*pz1, pz2 = dx*py1 - dy*px1;
        float t = LINE_THICKNESS;
        float qx1 = px1*t, qy1 = py1*t, qz1 = pz1*t;
        float qx2 = px2*t, qy2 = py2*t, qz2 = pz2*t;
        // 四个四边形
        buffer.addVertex(matrix, x1+qx1, y1+qy1, z1+qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1+qx2, y1+qy2, z1+qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2+qx2, y2+qy2, z2+qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2+qx1, y2+qy1, z2+qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1+qx2, y1+qy2, z1+qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1-qx1, y1-qy1, z1-qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2-qx1, y2-qy1, z2-qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2+qx2, y2+qy2, z2+qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1-qx1, y1-qy1, z1-qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1-qx2, y1-qy2, z1-qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2-qx2, y2-qy2, z2-qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2-qx1, y2-qy1, z2-qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1-qx2, y1-qy2, z1-qz2).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x1+qx1, y1+qy1, z1+qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2+qx1, y2+qy1, z2+qz1).setColor(color[0],color[1],color[2],color[3]);
        buffer.addVertex(matrix, x2-qx2, y2-qy2, z2-qz2).setColor(color[0],color[1],color[2],color[3]);
    }

    private static void addBoxThickLines(BufferBuilder buffer, Matrix4f matrix,
                                         float minX, float minY, float minZ,
                                         float maxX, float maxY, float maxZ,
                                         float[] color) {
        // 底面
        addThickLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        addThickLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        addThickLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        // 顶面
        addThickLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        addThickLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        addThickLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        // 竖边
        addThickLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        addThickLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        addThickLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private static void renderPreviewBox(Matrix4f matrix, BlockPos pastePos, int width, int height, int length, float[] color) {
        BufferBuilder buffer = setupLineRenderState();
        int minX = pastePos.getX();
        int minY = pastePos.getY();
        int minZ = pastePos.getZ();
        int maxX = minX + width;
        int maxY = minY + height;
        int maxZ = minZ + length;
        addBoxThickLines(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, color);
        finishRender(buffer);
    }


    // 使用 BlockRenderDispatcher.renderBatched 渲染真实方块模型（整体半透明着色，非弃用路径）

    private static void renderModelPreview(PoseStack poseStack, BlockPos pastePos, List<BlockEntry> entries) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        // 使用独立的 Immediate 缓冲，避免与世界渲染批次耦合
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(256));
        // 启用混合并降低整体 Alpha，实现“幽灵”效果
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.55f);
        // 轻量深度偏移，进一步避免 Z-fighting
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -10f);
        for (BlockEntry e : entries) {
            BlockPos worldPos = pastePos.offset(e.x, e.y, e.z);
            // 强制使用透明层，确保在 AFTER_TRANSLUCENT_BLOCKS 阶段可见
            VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
            // 先平移到方块世界坐标，再以中心缩放，避免 Z-fighting
            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());
            float s = 0.98f; // 2% 收缩
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.scale(s, s, s);
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            dispatcher.renderBatched(e.state, worldPos, level, poseStack, vc, false, level.random);
            poseStack.popPose();
        }
        // 只需刷新透明层即可
        bufferSource.endBatch(RenderType.translucent());
        RenderSystem.disablePolygonOffset();
        // 复原渲染状态
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static int[] getSchemSizeCached(String filename) {
        int[] cached = SCHEM_SIZE_CACHE.get(filename);
        if (cached != null) return cached;
        CompoundTag root = SchemSerializer.readSchem(filename);
        if (root == null) return null;
        CompoundTag schematic = root.getCompound("Schematic");
        if (schematic == null) return null;
        int w = schematic.getShort("Width");
        int h = schematic.getShort("Height");
        int l = schematic.getShort("Length");
        int[] size = new int[]{w, h, l};
        SCHEM_SIZE_CACHE.put(filename, size);
        return size;
    }


    // 模型缓存：包含方块相对坐标与 BlockState
    private static List<BlockEntry> getModelPreviewDataCached(String filename) {
        List<BlockEntry> cached = SCHEM_MODEL_CACHE.get(filename);
        if (cached != null) return cached;
        CompoundTag root = SchemSerializer.readSchem(filename);
        if (root == null) return null;
        CompoundTag schematic = root.getCompound("Schematic");
        if (schematic == null || schematic.isEmpty()) return null;
        int width = schematic.getShort("Width");
        int height = schematic.getShort("Height");
        int length = schematic.getShort("Length");
        CompoundTag blocksContainer = schematic.getCompound("Blocks");
        if (blocksContainer.isEmpty()) return null;
        Map<Integer, BlockState> palette = PaletteUtil.parsePalette(blocksContainer.getCompound("Palette"));
        byte[] dataBytes = blocksContainer.getByteArray("Data");
        List<Integer> data = VarIntUtil.decodeVarintArray(dataBytes);
        List<BlockEntry> list = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    if (list.size() >= MAX_PREVIEW_BLOCKS) break;
                    int idx = x + z * width + y * width * length;
                    if (idx >= data.size()) break;
                    Integer paletteIndex = data.get(idx);
                    BlockState state = palette.get(paletteIndex);
                    if (state != null && !state.isAir()) {
                        list.add(new BlockEntry(x, y, z, state));
                    }
                }
            }
        }
        SCHEM_MODEL_CACHE.put(filename, list);
        return list;
    }

    // 计算 .schem 的轻量级指纹，用于侦测同名文件被覆盖更新
    private static Long computeSchemFingerprint(String filename) {
        CompoundTag root = SchemSerializer.readSchem(filename);
        if (root == null) return null;
        CompoundTag schematic = root.getCompound("Schematic");
        if (schematic == null || schematic.isEmpty()) return null;
        int w = schematic.getShort("Width");
        int h = schematic.getShort("Height");
        int l = schematic.getShort("Length");
        CompoundTag blocks = schematic.getCompound("Blocks");
        int paletteSize = 0;
        int dataLen = 0;
        byte[] dataBytes = null;
        int blockEntityCount = 0;
        if (blocks != null && !blocks.isEmpty()) {
            CompoundTag pal = blocks.getCompound("Palette");
            if (pal != null) {
                paletteSize = pal.getAllKeys().size();
            }
            dataBytes = blocks.getByteArray("Data");
            if (dataBytes != null) dataLen = dataBytes.length;
            if (blocks.contains("BlockEntities")) {
                // 计数方块实体数量
                blockEntityCount = blocks.getList("BlockEntities", 10 /* TAG_Compound */).size();
            }
        }
        // 更强指纹：结构尺寸 + paletteSize + dataLen + BlockEntities 数 + 全量 Data 哈希
        int dataHash = (dataBytes != null && dataBytes.length > 0) ? Arrays.hashCode(dataBytes) : 0;
        long hash = 1469598103934665603L; // FNV-1a 64-bit offset basis
        final long prime = 1099511628211L;
        hash ^= (w & 0xFFFF); hash *= prime;
        hash ^= (h & 0xFFFF); hash *= prime;
        hash ^= (l & 0xFFFF); hash *= prime;
        hash ^= (paletteSize & 0xFFFFFFFFL); hash *= prime;
        hash ^= (dataLen & 0xFFFFFFFFL); hash *= prime;
        hash ^= (blockEntityCount & 0xFFFFFFFFL); hash *= prime;
        hash ^= (dataHash & 0xFFFFFFFFL); hash *= prime;
        return hash;
    }
}
