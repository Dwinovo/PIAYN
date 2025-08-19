package com.dwinovo.piayn.client.renderer.schematic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.dwinovo.piayn.client.renderer.catnip.outliner.Outliner;
import com.dwinovo.piayn.client.renderer.catnip.render.DefaultSuperRenderTypeBuffer;
import com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.mojang.math.Axis;

/**
 * 蓝图预览渲染器（精简版）。
 *
 * 设计要点：
 * - 仅负责渲染（不包含模板加载与生命周期管理），符合“关注点分离”。
 * - {@link com.dwinovo.piayn.world.schematic.level.SchematicLevel} 中的方块数据均为“相对(0,0,0)”坐标，
 *   本类通过维护一个可平滑插值的锚点（anchor）将这些相对坐标转换到世界坐标进行渲染。
 * - 逐层遍历 {@link net.minecraft.client.renderer.RenderType#chunkBufferLayers()}，按层写入传入的 {@link com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer}。
 * - 适配 NeoForge 1.21.1：在 {@code RenderLevelStageEvent.AFTER_PARTICLES} 阶段进行绘制，
 *   并通过事件提供的相机位置对 {@link com.mojang.blaze3d.vertex.PoseStack} 做相机平移，以避免重复加减摄像机偏移。
 */
public class SchematicPreviewRenderer {
    private Vec3 prevAnchor = Vec3.ZERO;
    private Vec3 currAnchor = Vec3.ZERO;
    private Vec3 targetAnchor = Vec3.ZERO;

    // 以某个世界坐标为中心的旋转设置（由外部事件设置），单位：度
    private Vec3 rotationCenter = null;
    private float rotationDegrees = 0f;

    // 幽灵渲染参数（可日后做成可配置项）
    private static final float GHOST_ALPHA = 0.55f; // 半透明强度
    private static final float TINT_R = 0.9f;       // 轻微偏冷的高亮蓝白色
    private static final float TINT_G = 0.95f;
    private static final float TINT_B = 1.0f;

    private static final Object OUTLINE_SLOT = new Object();

    // 日志
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 每帧/每tick推进一次锚点的“追踪”插值。
     *
     * 机制说明：
     * - 使用 {@link net.minecraft.util.Mth#lerp} 以 0.05f 的系数将当前位置向目标位置逼近，
     *   形成“追逐”效果，参考了 Create 模组中 ChasingAABBOutline 的实现风格。
     * - 存档：{@code prevAnchor} 用于与 {@code currAnchor} 做 partialTick 插值，保证运动平滑。
     */
    private void tickTowards(Vec3 target) {
        // 更新追踪目标
        this.targetAnchor = target;
        // 首次使用时（或发生显著跳变）将当前/上一帧对齐到目标，避免冷启动抖动
        if (this.prevAnchor.equals(Vec3.ZERO) && this.currAnchor.equals(Vec3.ZERO)) {
            this.prevAnchor = target;
            this.currAnchor = target;
        }
        this.prevAnchor = this.currAnchor;
        // chase factor 0.5f as in ChasingAABBOutline#tick
        this.currAnchor = new Vec3(
            Mth.lerp(0.05f, this.currAnchor.x, this.targetAnchor.x),
            Mth.lerp(0.05f, this.currAnchor.y, this.targetAnchor.y),
            Mth.lerp(0.05f, this.currAnchor.z, this.targetAnchor.z)
        );
    }

    /**
     * 执行实际渲染。
     *
     * 参数说明：
     * - {@code ms}: 调用方（事件）已经应用了摄像机偏移（-cam），故此处只需平移到目标世界坐标。
     * - {@code buffers}: 自定义的缓冲管理器，内部包装了不同 {@link RenderType} 的缓冲。
     * - {@code partialTick}: 使用 NeoForge 1.21 的 {@code DeltaTracker#getGameTimeDeltaPartialTick(false)} 提供的值。
     * - {@code schematic}: 局部坐标系下的数据来源（相对(0,0,0)）。
     * - {@code anchor}: 目标锚点位置（世界坐标基准）。
     *
     * 实现细节：
     * - 先根据 {@code prevAnchor}/{@code currAnchor} 与 partialTick 计算插值锚点，并拆分为整数基准 + 小数渲染偏移，
     *   保证方块网格对齐，避免模型因浮点偏移导致抖动。
     * - 遍历结构包围盒 {@link BoundingBox} 中的每个局部方块位置，查询方块状态与模型数据，以半透明层进行一次性渲染（幽灵效果）。
     */
    public void render(PoseStack ms, SuperRenderTypeBuffer buffers, float partialTick, SchematicLevel schematicLevel, BlockPos anchor) {
        if (schematicLevel == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        ModelBlockRenderer renderer = dispatcher.getModelRenderer();
        RandomSource random = RandomSource.createNewThreadLocalInstance();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BoundingBox bounds = schematicLevel.getBounds();

        // 记录局部体素边界（相对原点）
        int minX = bounds.minX(), minY = bounds.minY(), minZ = bounds.minZ();
        int maxX = bounds.maxX(), maxY = bounds.maxY(), maxZ = bounds.maxZ();


        // 推进一次追踪到目标锚点
        tickTowards(Vec3.atLowerCornerOf(anchor));

        // 根据上一帧/当前帧 anchor 计算插值，获得平滑移动的渲染锚点
        float pt = partialTick;
        Vec3 interpAnchor = new Vec3(
            Mth.lerp(pt, this.prevAnchor.x, this.currAnchor.x),
            Mth.lerp(pt, this.prevAnchor.y, this.currAnchor.y),
            Mth.lerp(pt, this.prevAnchor.z, this.currAnchor.z)
        );
        // 使用插值后的锚点构造含平滑过渡的小数坐标的 AABB（上界开区间：max+1）
        double minXd = minX + interpAnchor.x;
        double minYd = minY + interpAnchor.y;
        double minZd = minZ + interpAnchor.z;
        double maxXd = maxX + 1 + interpAnchor.x;
        double maxYd = maxY + 1 + interpAnchor.y;
        double maxZd = maxZ + 1 + interpAnchor.z;
        AABB previewAABB = new AABB(minXd, minYd, minZd, maxXd, maxYd, maxZd);
        Outliner.getInstance().showAABB(OUTLINE_SLOT, previewAABB)
                .colored(0x6886c5)
                .lineWidth(1 / 16f);
        // 拆分为整数网格基准与小数偏移：保持方块坐标稳定，小数部分作为渲染平移偏移
        Vec3 baseAnchor = Vec3.atLowerCornerOf(anchor);
        Vec3 renderOffset = interpAnchor.subtract(baseAnchor);

        ModelBlockRenderer.enableCaching();

        // 若设置了围绕某中心的旋转，则在渲染所有方块前对坐标系进行一次性旋转（绕 Y 轴）
        boolean appliedRotation = false;
        if (this.rotationCenter != null && this.rotationDegrees != 0f) {
            ms.pushPose();
            ms.translate(rotationCenter.x, rotationCenter.y, rotationCenter.z);
            ms.mulPose(Axis.YP.rotationDegrees(this.rotationDegrees));
            ms.translate(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z);
            appliedRotation = true;
        }

        // 幽灵渲染：统一以半透明层输出，并对 RGBA 进行衰减（不再按模型层筛选）
        VertexConsumer ghostConsumer = new TintingVertexConsumer(buffers.getBuffer(RenderType.translucent()), TINT_R, TINT_G, TINT_B, GHOST_ALPHA);
        for (BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            // 局部坐标查询：SchematicLevel 仅存储相对原点的数据
            BlockState state = schematicLevel.getBlockState(localPos);
            if (state.getRenderShape() != RenderShape.MODEL)
                continue;

            // 计算世界坐标位置：world = local + anchor
            BlockPos worldPos = mutable.setWithOffset(localPos, anchor);

            BakedModel model = dispatcher.getBlockModel(state);
            BlockEntity be = schematicLevel.getBlockEntity(localPos);
            ModelData modelData = be != null ? be.getModelData() : ModelData.EMPTY;
            modelData = model.getModelData(schematicLevel, localPos, state, modelData);
            long seed = state.getSeed(worldPos);
            random.setSeed(seed);

            ms.pushPose();
            // 将局部方块平移到世界位置，并加上渲染的小数偏移；相机偏移由调用方已处理
            ms.translate(worldPos.getX() + renderOffset.x, worldPos.getY() + renderOffset.y, worldPos.getZ() + renderOffset.z);
            renderer.tesselateBlock(
                schematicLevel,
                model,
                state,
                localPos,
                ms,
                ghostConsumer,
                true,
                random,
                seed,
                OverlayTexture.NO_OVERLAY,
                modelData,
                RenderType.translucent()
            );
            ms.popPose();
        }

        // 清理模型渲染缓存，恢复 schematic 的渲染模式标志
        ModelBlockRenderer.clearCache();

        if (appliedRotation) {
            ms.popPose();
        }
    }

    /**
     * 设置围绕指定世界坐标的旋转（度）。
     */
    public void setRotationAround(Vec3 worldCenter, float degrees) {
        this.rotationCenter = worldCenter;
        this.rotationDegrees = degrees;
    }

    /**
     * 从渲染事件直接执行一次蓝图预览渲染：
     * - 处理相机平移
     * - 从事件的 DeltaTracker 提取 partial tick
     * - 使用默认的 SuperRenderTypeBuffer
     */
    public void renderFromEvent(RenderLevelStageEvent event, SchematicLevel level, BlockPos anchor) {
        if (level == null)
            return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ms = event.getPoseStack();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        ms.pushPose();
        ms.translate(-cam.x, -cam.y, -cam.z);
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        this.render(ms, buffers, pt, level, anchor);
        ms.popPose();
        buffers.draw();
    }

    /**
     * 顶点消费者包装器：对传入的 RGBA 进行统一乘法（用于幽灵半透明/色调）。
     * 注意：该实现依赖于标准 Model 渲染管线按顶点流式调用 color(...)，若底层使用 putBulkData 直接写入，
     * 则颜色缩放可能不生效；在常见方块模型上此法可工作，若发现个别方块例外，可再做专项适配。
     */
    private static class TintingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float rMul, gMul, bMul, aMul;

        TintingVertexConsumer(VertexConsumer delegate, float rMul, float gMul, float bMul, float aMul) {
            this.delegate = delegate;
            this.rMul = rMul;
            this.gMul = gMul;
            this.bMul = bMul;
            this.aMul = aMul;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return delegate.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            int rr = Mth.clamp((int) (r * rMul), 0, 255);
            int gg = Mth.clamp((int) (g * gMul), 0, 255);
            int bb = Mth.clamp((int) (b * bMul), 0, 255);
            int aa = Mth.clamp((int) (a * aMul), 0, 255);
            return delegate.setColor(rr, gg, bb, aa);
        }

        @Override
        public VertexConsumer setUv(float u, float v) { return delegate.setUv(u, v); }

        @Override
        public VertexConsumer setUv1(int u, int v) { return delegate.setUv1(u, v); }

        @Override
        public VertexConsumer setUv2(int u, int v) { return delegate.setUv2(u, v); }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) { return delegate.setNormal(x, y, z); }

        // 1.21 VertexConsumer 无 endVertex/defaultColor/unsetDefaultColor，这里不实现
    }
}
