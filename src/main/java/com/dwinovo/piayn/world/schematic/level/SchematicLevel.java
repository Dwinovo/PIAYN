package com.dwinovo.piayn.world.schematic.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

/**
 * 仅用于“蓝图数据”的轻量级关卡视图。
 *
 * 设计原则：
 * - 只存储“相对(0,0,0)”坐标下的方块/方块实体/实体数据，不关心锚点。
 *   由渲染器（如 {@code SchematicPreviewRenderer}）将局部坐标 + anchor 转为世界坐标。
 * - 提供最小化的 Level/ServerLevelAccessor 能力，以便复用 vanilla 的模型/方块渲染管线。
 * - 不参与真实世界交互：光照恒定、刻（tick）被黑洞化、玩家列表为空等。
 */
public class SchematicLevel extends WrappedLevel implements ServerLevelAccessor, ISchematicLevelAccessor {
    protected Map<BlockPos, BlockState> blocks;
    protected Map<BlockPos, BlockEntity> blockEntities;
    protected List<BlockEntity> renderedBlockEntities;
    protected List<Entity> entities;
    protected BoundingBox bounds;

    public boolean renderMode;

    /**
     * 构造：包裹一个真实的 {@link Level} 以复用其注册表/资源等上下文。
     * 创建自定义的 {@link SchematicChunkSource} 避免真实区块依赖。
     */
    public SchematicLevel(Level original) {
        super(original);
        setChunkSource(new SchematicChunkSource(this));
        this.blocks = new HashMap<>();
        this.blockEntities = new HashMap<>();
        this.bounds = new BoundingBox(BlockPos.ZERO);
        this.entities = new ArrayList<>();
        this.renderedBlockEntities = new ArrayList<>();
    }

    @Override
    public Set<BlockPos> getAllPositions() {
        return blocks.keySet();
    }

    @Override
    public boolean addFreshEntity(Entity entityIn) {
        return entities.add(entityIn);
    }

    @Override
    public List<Entity> getEntityList() {
        return entities;
    }

    @Override
    /**
     * 懒创建方块实体：
     * - 若该位置存在需要方块实体的方块，按需 new 并缓存在 {@code blockEntities}。
     * - 同时将其加入 {@code renderedBlockEntities}，便于渲染端遍历（如需要）。
     */
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (isOutsideBuildHeight(pos))
            return null;
        if (blockEntities.containsKey(pos))
            return blockEntities.get(pos);
        if (!blocks.containsKey(pos))
            return null;

        BlockState blockState = getBlockState(pos);
        if (blockState.hasBlockEntity()) {
            BlockEntity blockEntity = ((EntityBlock) blockState.getBlock()).newBlockEntity(pos, blockState);
            if (blockEntity != null) {
                onBEadded(blockEntity, pos);
                blockEntities.put(pos, blockEntity);
                renderedBlockEntities.add(blockEntity);
            }
            return blockEntity;
        }
        return null;
    }

    /**
     * 当方块实体被添加时，补上其 level 引用为本 SchematicLevel。
     */
    protected void onBEadded(BlockEntity blockEntity, BlockPos pos) {
        blockEntity.setLevel(this);
    }

    @Override
    /**
     * 查询局部坐标下的方块状态。
     * - 若在蓝图下方一层（y == bounds.minY - 1）且非渲染模式，返回一层泥土以方便“打印”或预览对比。
     * - 若在 bounds 内并存在则返回处理后的状态，否则为空气。
     *
     * 提示：渲染期间会设置 {@link #renderMode} = true，避免出现“打印辅助层”。
     */
    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() - bounds.minY() == -1 && !renderMode)
            return Blocks.DIRT.defaultBlockState();
        if (getBounds().isInside(pos) && blocks.containsKey(pos))
            return processBlockStateForPrinting(blocks.get(pos));
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public Map<BlockPos, BlockState> getBlockMap() {
        return blocks;
    }

    @Override
    /**
     * 流体状态直接由方块状态提供。
     */
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    /**
     * 统一返回平原生物群系，保证模型颜色/草色等可解析即可。
     */
    public Holder<Biome> getBiome(BlockPos pos) {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    }

    @Override
    /**
     * 简化光照：恒为满亮度，避免真实光照查询。
     */
    public int getBrightness(LightLayer lightLayer, BlockPos pos) {
        return 15;
    }

    @Override
    /**
     * 取消阴影衰减，保证预览清晰。
     */
    public float getShade(Direction face, boolean hasShade) {
        return 1f;
    }

    @Override
    /**
     * 刻管理黑洞：不产生真实 tick。
     */
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    /** 同上，流体刻黑洞。 */
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public List<Entity> getEntities(Entity arg0, AABB arg1, Predicate<? super Entity> arg2) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> arg0, AABB arg1, Predicate<? super T> arg2) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends Player> players() {
        return Collections.emptyList();
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(getBlockState(pos));
    }

    @Override
    public boolean destroyBlock(BlockPos arg0, boolean arg1) {
        return setBlock(arg0, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean removeBlock(BlockPos arg0, boolean arg1) {
        return setBlock(arg0, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    /**
     * 设置局部方块并更新包围盒。
     * - 每次写入都会扩展 {@link #bounds} 以包含该位置，用于后续遍历渲染与区域判断。
     * - 若方块类型变化导致原有方块实体无效，则清理对应缓存。
     * - 最后尝试懒创建方块实体并缓存。
     */
    public boolean setBlock(BlockPos pos, BlockState arg1, int arg2) {
        pos = pos.immutable();
        // expand bounds to include pos
        int minX = Math.min(bounds.minX(), pos.getX());
        int minY = Math.min(bounds.minY(), pos.getY());
        int minZ = Math.min(bounds.minZ(), pos.getZ());
        int maxX = Math.max(bounds.maxX(), pos.getX());
        int maxY = Math.max(bounds.maxY(), pos.getY());
        int maxZ = Math.max(bounds.maxZ(), pos.getZ());
        bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        blocks.put(pos, arg1);
        if (blockEntities.containsKey(pos)) {
            BlockEntity blockEntity = blockEntities.get(pos);
            if (!blockEntity.getType()
                    .isValid(arg1)) {
                blockEntities.remove(pos);
                renderedBlockEntities.remove(blockEntity);
            }
        }

        BlockEntity blockEntity = getBlockEntity(pos);
        if (blockEntity != null)
            blockEntities.put(pos, blockEntity);

        return true;
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}

    @Override
    public BoundingBox getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(BoundingBox bounds) {
        this.bounds = bounds;
    }

    @Override
    public Iterable<BlockEntity> getBlockEntities() {
        return blockEntities.values();
    }

    @Override
    public Iterable<BlockEntity> getRenderedBlockEntities() {
        return renderedBlockEntities;
    }

    /**
     * 针对“打印/预览”的状态调整：
     * - 例如关闭熔炉一类方块的 LIT 属性，避免预览时出现“点亮”假象。
     */
    protected BlockState processBlockStateForPrinting(BlockState state) {
        if (state.getBlock() instanceof AbstractFurnaceBlock && state.hasProperty(BlockStateProperties.LIT))
            state = state.setValue(BlockStateProperties.LIT, false);
        return state;
    }

    @Override
    /**
     * 此方法仅在服务端环境可用；客户端直接调用会抛错。
     * 之所以保留，是为了与某些 vanilla/NeoForge 接口兼容。
     */
    public ServerLevel getLevel() {
        if (this.level instanceof ServerLevel) {
            return (ServerLevel) this.level;
        }
        throw new IllegalStateException("Cannot use IServerWorld#getWorld in a client environment");
    }
}
