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
 * 一个精简版的 Create SchematicRenderer：
 * - 不做工具与镜像，只做直接渲染
 * - 逐层遍历 RenderType.chunkBufferLayers()
 * - 直接把几何写入传入的 SuperRenderTypeBuffer
 */
public class SchematicPreviewRenderer {
    private SchematicLevel schematic;
    private BlockPos anchor = BlockPos.ZERO;
    private boolean active = false;

    private Vec3 prevAnchor = Vec3.ZERO;
    private Vec3 currAnchor = Vec3.ZERO;
    private Vec3 targetAnchor = Vec3.ZERO;
    private boolean anchorInitialized = false;

    public void display(SchematicLevel level) {
        this.schematic = level;
        this.anchor = level.anchor;
        this.active = true;

        Vec3 a = Vec3.atLowerCornerOf(this.anchor);
        if (!anchorInitialized) {
            this.prevAnchor = a;
            this.currAnchor = a;
            this.targetAnchor = a;
            this.anchorInitialized = true;
        }
    }

    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor;
        if (this.schematic != null) {
            this.schematic.anchor = anchor;
        }

        // Update target anchor for chasing
        this.targetAnchor = Vec3.atLowerCornerOf(anchor);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Progress the chasing anchor once per frame/tick. */
    public void tick() {
        if (!anchorInitialized) {
            Vec3 a = Vec3.atLowerCornerOf(this.anchor);
            this.prevAnchor = a;
            this.currAnchor = a;
            this.targetAnchor = a;
            this.anchorInitialized = true;
        }
        this.prevAnchor = this.currAnchor;
        // chase factor 0.5f as in ChasingAABBOutline#tick
        this.currAnchor = new Vec3(
            Mth.lerp(0.05f, this.currAnchor.x, this.targetAnchor.x),
            Mth.lerp(0.05f, this.currAnchor.y, this.targetAnchor.y),
            Mth.lerp(0.05f, this.currAnchor.z, this.targetAnchor.z)
        );
    }

    public void render(PoseStack ms, SuperRenderTypeBuffer buffers, float partialTick) {
        if (!active || schematic == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        ModelBlockRenderer renderer = dispatcher.getModelRenderer();
        RandomSource random = RandomSource.createNewThreadLocalInstance();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BoundingBox bounds = schematic.getBounds();

        // Interpolated anchor for smooth movement
        float pt = partialTick;
        Vec3 interpAnchor = new Vec3(
            Mth.lerp(pt, this.prevAnchor.x, this.currAnchor.x),
            Mth.lerp(pt, this.prevAnchor.y, this.currAnchor.y),
            Mth.lerp(pt, this.prevAnchor.z, this.currAnchor.z)
        );
        // Integer base to keep block coordinates stable; render offset is the fractional delta
        Vec3 baseAnchor = Vec3.atLowerCornerOf(this.anchor);
        Vec3 renderOffset = interpAnchor.subtract(baseAnchor);

        schematic.renderMode = true;
        ModelBlockRenderer.enableCaching();

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            for (BlockPos localPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                BlockPos worldPos = mutable.setWithOffset(localPos, anchor);
                BlockState state = schematic.getBlockState(worldPos);
                if (state.getRenderShape() != RenderShape.MODEL)
                    continue;

                BakedModel model = dispatcher.getBlockModel(state);
                BlockEntity be = schematic.getBlockEntity(worldPos);
                ModelData modelData = be != null ? be.getModelData() : ModelData.EMPTY;
                modelData = model.getModelData(schematic, worldPos, state, modelData);
                long seed = state.getSeed(worldPos);
                random.setSeed(seed);
                if (!model.getRenderTypes(state, random, modelData).contains(layer))
                    continue;

                ms.pushPose();
                // translate to world block position plus smooth render offset; camera offset is applied by caller
                ms.translate(worldPos.getX() + renderOffset.x, worldPos.getY() + renderOffset.y, worldPos.getZ() + renderOffset.z);
                renderer.tesselateBlock(
                    schematic,
                    model,
                    state,
                    worldPos,
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

        ModelBlockRenderer.clearCache();
        schematic.renderMode = false;
    }
}
