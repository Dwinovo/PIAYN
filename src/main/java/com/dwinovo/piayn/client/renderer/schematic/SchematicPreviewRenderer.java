package com.dwinovo.piayn.client.renderer.schematic;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.world.phys.Vec3;

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
     * - 遍历结构包围盒 {@link BoundingBox} 中的每个局部方块位置，查询方块状态与模型数据，按照模型声明的 render layer 选择性渲染。
     */
    public void render(PoseStack ms, SuperRenderTypeBuffer buffers, float partialTick, SchematicLevel schematic, BlockPos anchor) {
        if (schematic == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        ModelBlockRenderer renderer = dispatcher.getModelRenderer();
        RandomSource random = RandomSource.createNewThreadLocalInstance();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BoundingBox bounds = schematic.getBounds();

        // 推进一次追踪到目标锚点
        tickTowards(Vec3.atLowerCornerOf(anchor));

        // 根据上一帧/当前帧 anchor 计算插值，获得平滑移动的渲染锚点
        float pt = partialTick;
        Vec3 interpAnchor = new Vec3(
            Mth.lerp(pt, this.prevAnchor.x, this.currAnchor.x),
            Mth.lerp(pt, this.prevAnchor.y, this.currAnchor.y),
            Mth.lerp(pt, this.prevAnchor.z, this.currAnchor.z)
        );
        // 拆分为整数网格基准与小数偏移：保持方块坐标稳定，小数部分作为渲染平移偏移
        Vec3 baseAnchor = Vec3.atLowerCornerOf(anchor);
        Vec3 renderOffset = interpAnchor.subtract(baseAnchor);

        schematic.renderMode = true;
        ModelBlockRenderer.enableCaching();

        // 逐层渲染：仅当模型声明会在该 RenderType 下渲染时才绘制
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            for (BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                // 局部坐标查询：SchematicLevel 仅存储相对原点的数据
                BlockState state = schematic.getBlockState(localPos);
                if (state.getRenderShape() != RenderShape.MODEL)
                    continue;

                // 计算世界坐标位置：world = local + anchor
                BlockPos worldPos = mutable.setWithOffset(localPos, anchor);
                
                BakedModel model = dispatcher.getBlockModel(state);
                BlockEntity be = schematic.getBlockEntity(localPos);
                ModelData modelData = be != null ? be.getModelData() : ModelData.EMPTY;
                modelData = model.getModelData(schematic, localPos, state, modelData);
                long seed = state.getSeed(worldPos);
                random.setSeed(seed);
                if (!model.getRenderTypes(state, random, modelData).contains(layer))
                    continue;

                ms.pushPose();
                // 将局部方块平移到世界位置，并加上渲染的小数偏移；相机偏移由调用方已处理
                ms.translate(worldPos.getX() + renderOffset.x, worldPos.getY() + renderOffset.y, worldPos.getZ() + renderOffset.z);
                renderer.tesselateBlock(
                    schematic,
                    model,
                    state,
                    localPos,
                    ms,
                    buffers.getBuffer(layer),
                    true,
                    random,
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    layer
                );
                ms.popPose();
            }
        }

        // 清理模型渲染缓存，恢复 schematic 的渲染模式标志
        ModelBlockRenderer.clearCache();
        schematic.renderMode = false;
    }
}

