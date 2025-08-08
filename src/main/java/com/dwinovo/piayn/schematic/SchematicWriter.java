package com.dwinovo.piayn.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Sponge Schematic Format (.schem) 文件写入器
 * 基于 Sponge Schematic Specification v3
 * 
 * 功能：
 * - 将Minecraft世界区域保存为.schem格式文件
 * - 完全兼容WorldEdit等主流工具
 * - 支持方块、方块实体、实体的完整保存
 * 
 * @author Dwinovo
 * @version 1.0
 * @since 2025-01-08
 */
public final class SchematicWriter {
    
    // === 格式常量 ===
    private static final int SCHEMATIC_VERSION = 3;
    private static final String SCHEMATIC_KEY = "Schematic";
    
    // === 字段名常量 ===
    private static final String VERSION_KEY = "Version";
    private static final String DATA_VERSION_KEY = "DataVersion";
    private static final String WIDTH_KEY = "Width";
    private static final String HEIGHT_KEY = "Height";
    private static final String LENGTH_KEY = "Length";
    private static final String OFFSET_KEY = "Offset";
    private static final String METADATA_KEY = "Metadata";
    private static final String BLOCKS_KEY = "Blocks";
    private static final String ENTITIES_KEY = "Entities";
    
    // === Block Container 字段 ===
    private static final String PALETTE_KEY = "Palette";
    private static final String DATA_KEY = "Data";
    private static final String BLOCK_ENTITIES_KEY = "BlockEntities";
    
    // === Metadata 字段 ===
    private static final String NAME_KEY = "Name";
    private static final String AUTHOR_KEY = "Author";
    private static final String DATE_KEY = "Date";
    
    // === 私有构造函数，工具类不允许实例化 ===
    private SchematicWriter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 将指定区域保存为.schem文件
     * 
     * @param serverLevel 服务器世界
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @param outputPath 输出文件路径
     * @param schematicName 原理图名称（可选）
     * @param authorName 作者名称（可选）
     * @throws IOException 文件写入异常
     */
    public static void writeSchematic(@Nonnull ServerLevel serverLevel, 
                                    @Nonnull BlockPos startPos, 
                                    @Nonnull BlockPos endPos, 
                                    @Nonnull Path outputPath, 
                                    @Nullable String schematicName, 
                                    @Nullable String authorName) throws IOException {
        
        // 计算区域边界
        final int minX = Math.min(startPos.getX(), endPos.getX());
        final int minY = Math.min(startPos.getY(), endPos.getY());
        final int minZ = Math.min(startPos.getZ(), endPos.getZ());
        final int maxX = Math.max(startPos.getX(), endPos.getX());
        final int maxY = Math.max(startPos.getY(), endPos.getY());
        final int maxZ = Math.max(startPos.getZ(), endPos.getZ());
        
        final int width = maxX - minX + 1;
        final int height = maxY - minY + 1;
        final int length = maxZ - minZ + 1;
        
        final BlockPos originPos = new BlockPos(minX, minY, minZ);
        
        // 构建schematic NBT结构
        CompoundTag schematicTag = new CompoundTag();
        
        // 基本信息
        schematicTag.putInt(VERSION_KEY, SCHEMATIC_VERSION);
        schematicTag.putInt(DATA_VERSION_KEY, 3953); // Minecraft 1.21.1 data version
        schematicTag.putShort(WIDTH_KEY, (short) width);
        schematicTag.putShort(HEIGHT_KEY, (short) height);
        schematicTag.putShort(LENGTH_KEY, (short) length);
        schematicTag.put(OFFSET_KEY, new IntArrayTag(new int[]{0, 0, 0}));
        
        // 元数据
        if (schematicName != null || authorName != null) {
            CompoundTag metadataTag = new CompoundTag();
            if (schematicName != null) {
                metadataTag.putString(NAME_KEY, schematicName);
            }
            if (authorName != null) {
                metadataTag.putString(AUTHOR_KEY, authorName);
            }
            metadataTag.putLong(DATE_KEY, System.currentTimeMillis());
            schematicTag.put(METADATA_KEY, metadataTag);
        }
        
        // 构建方块容器
        CompoundTag blocksContainer = buildBlockContainer(serverLevel, originPos, width, height, length);
        schematicTag.put(BLOCKS_KEY, blocksContainer);
        
        // 构建实体列表
        ListTag entitiesList = buildEntitiesList(serverLevel, originPos, width, height, length);
        if (!entitiesList.isEmpty()) {
            schematicTag.put(ENTITIES_KEY, entitiesList);
        }
        
        // 按照Sponge规范构建正确的NBT结构
        CompoundTag rootContent = new CompoundTag();
        rootContent.put(SCHEMATIC_KEY, schematicTag);
        
        // 写入文件
        writeCompressedNbt(rootContent, outputPath);
    }
    
    /**
     * 构建方块容器
     */
    private static CompoundTag buildBlockContainer(@Nonnull ServerLevel serverLevel, 
                                                 @Nonnull BlockPos originPos, 
                                                 int width, int height, int length) {
        
        // 调色板：方块状态 -> 索引
        Map<BlockState, Integer> paletteMap = new LinkedHashMap<>();
        List<Integer> blockData = new ArrayList<>();
        List<CompoundTag> blockEntitiesList = new ArrayList<>();
        
        // 遍历区域内所有方块
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos currentPos = originPos.offset(x, y, z);
                    BlockState blockState = serverLevel.getBlockState(currentPos);
                    
                    // 获取或创建调色板索引
                    Integer paletteIndex = paletteMap.get(blockState);
                    if (paletteIndex == null) {
                        paletteIndex = paletteMap.size();
                        paletteMap.put(blockState, paletteIndex);
                    }
                    blockData.add(paletteIndex);
                    
                    // 处理方块实体
                    BlockEntity blockEntity = serverLevel.getBlockEntity(currentPos);
                    if (blockEntity != null) {
                        CompoundTag blockEntityTag = createBlockEntityTag(serverLevel, blockEntity, x, y, z);
                        if (blockEntityTag != null) {
                            blockEntitiesList.add(blockEntityTag);
                        }
                    }
                }
            }
        }
        
        // 构建调色板NBT
        CompoundTag paletteTag = new CompoundTag();
        for (Map.Entry<BlockState, Integer> entry : paletteMap.entrySet()) {
            String blockStateString = blockStateToString(entry.getKey());
            paletteTag.putInt(blockStateString, entry.getValue());
        }
        
        // 构建方块容器
        CompoundTag blocksContainer = new CompoundTag();
        blocksContainer.put(PALETTE_KEY, paletteTag);
        blocksContainer.put(DATA_KEY, encodeVarintArray(blockData));
        
        if (!blockEntitiesList.isEmpty()) {
            ListTag blockEntitiesListTag = new ListTag();
            blockEntitiesList.forEach(blockEntitiesListTag::add);
            blocksContainer.put(BLOCK_ENTITIES_KEY, blockEntitiesListTag);
        }
        
        return blocksContainer;
    }
    
    /**
     * 构建实体列表
     */
    private static ListTag buildEntitiesList(@Nonnull ServerLevel serverLevel, 
                                           @Nonnull BlockPos originPos, 
                                           int width, int height, int length) {
        
        ListTag entitiesList = new ListTag();
        
        // 定义区域边界
        AABB regionBounds = new AABB(
            originPos.getX(), originPos.getY(), originPos.getZ(),
            originPos.getX() + width, originPos.getY() + height, originPos.getZ() + length
        );
        
        // 获取区域内的所有实体
        List<Entity> entitiesInRegion = serverLevel.getEntities((Entity) null, regionBounds, entity -> true);
        
        for (Entity entity : entitiesInRegion) {
            CompoundTag entityTag = createEntityTag(entity, originPos);
            if (entityTag != null) {
                entitiesList.add(entityTag);
            }
        }
        
        return entitiesList;
    }
    
    /**
     * 创建方块实体NBT标签
     */
    @Nullable
    private static CompoundTag createBlockEntityTag(@Nonnull ServerLevel serverLevel, @Nonnull BlockEntity blockEntity, int x, int y, int z) {
        try {
            CompoundTag blockEntityData = blockEntity.saveWithoutMetadata(serverLevel.registryAccess());
            if (blockEntityData.isEmpty()) {
                return null;
            }
            
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.putString("Id", getBlockEntityId(blockEntity));
            blockEntityTag.put("Pos", new IntArrayTag(new int[]{x, y, z}));
            blockEntityTag.put("Data", blockEntityData);
            
            return blockEntityTag;
            
        } catch (Exception e) {
            // 记录错误但不中断处理
            System.err.println("Failed to save block entity at " + x + "," + y + "," + z + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建实体NBT标签
     */
    @Nullable
    private static CompoundTag createEntityTag(@Nonnull Entity entity, @Nonnull BlockPos originPos) {
        try {
            CompoundTag entityData = new CompoundTag();
            entity.save(entityData);
            
            if (entityData.isEmpty()) {
                return null;
            }
            
            CompoundTag entityTag = new CompoundTag();
            
            // 计算相对位置
            double relativeX = entity.getX() - originPos.getX();
            double relativeY = entity.getY() - originPos.getY();
            double relativeZ = entity.getZ() - originPos.getZ();
            
            entityTag.put("Pos", new ListTag() {{
                add(DoubleTag.valueOf(relativeX));
                add(DoubleTag.valueOf(relativeY));
                add(DoubleTag.valueOf(relativeZ));
            }});
            
            ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entityType != null) {
                entityTag.putString("Id", entityType.toString());
            }
            
            entityTag.put("Data", entityData);
            
            return entityTag;
            
        } catch (Exception e) {
            System.err.println("Failed to save entity " + entity.getType() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 将方块状态转换为字符串表示
     */
    @Nonnull
    private static String blockStateToString(@Nonnull BlockState blockState) {
        Block block = blockState.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        
        if (blockState.getProperties().isEmpty()) {
            return blockId.toString();
        }
        
        StringBuilder stringBuilder = new StringBuilder(blockId.toString());
        stringBuilder.append('[');
        
        boolean isFirst = true;
        for (Property<?> property : blockState.getProperties()) {
            if (!isFirst) {
                stringBuilder.append(',');
            }
            stringBuilder.append(property.getName()).append('=').append(getPropertyValueString(blockState, property));
            isFirst = false;
        }
        
        stringBuilder.append(']');
        return stringBuilder.toString();
    }
    
    /**
     * 获取属性值字符串
     */
    @Nonnull
    private static <T extends Comparable<T>> String getPropertyValueString(@Nonnull BlockState blockState, @Nonnull Property<T> property) {
        T value = blockState.getValue(property);
        return property.getName(value);
    }
    
    /**
     * 获取方块实体ID
     */
    @Nonnull
    private static String getBlockEntityId(@Nonnull BlockEntity blockEntity) {
        ResourceLocation blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return blockEntityType != null ? blockEntityType.toString() : "unknown";
    }
    
    /**
     * 编码varint数组
     */
    @Nonnull
    private static ByteArrayTag encodeVarintArray(@Nonnull List<Integer> data) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            
            for (Integer value : data) {
                writeVarint(dataOutputStream, value);
            }
            
            return new ByteArrayTag(byteArrayOutputStream.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode varint array", e);
        }
    }
    
    /**
     * 写入varint值
     */
    private static void writeVarint(@Nonnull DataOutputStream outputStream, int value) throws IOException {
        while ((value & 0x80) != 0) {
            outputStream.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        outputStream.writeByte(value & 0x7F);
    }
    
    /**
     * 写入压缩的NBT文件
     */
    private static void writeCompressedNbt(@Nonnull CompoundTag nbtTag, @Nonnull Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        NbtIo.writeCompressed(nbtTag, filePath);
    }
}
