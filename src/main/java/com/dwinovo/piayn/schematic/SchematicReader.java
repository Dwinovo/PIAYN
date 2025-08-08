package com.dwinovo.piayn.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Sponge Schematic Format (.schem) 文件读取器
 * 基于 Sponge Schematic Specification v3
 * 
 * 功能：
 * - 读取和解析.schem格式文件
 * - 将schematic数据粘贴到Minecraft世界中
 * - 完全兼容WorldEdit等主流工具生成的文件
 * 
 * @author Dwinovo
 * @version 1.0
 * @since 2025-01-08
 */
public final class SchematicReader {
    
    // === 字段名常量 ===
    private static final String SCHEMATIC_KEY = "Schematic";
    private static final String VERSION_KEY = "Version";
    private static final String DATA_VERSION_KEY = "DataVersion";
    private static final String WIDTH_KEY = "Width";
    private static final String HEIGHT_KEY = "Height";
    private static final String LENGTH_KEY = "Length";

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
    
    // === 私有构造函数，工具类不允许实例化 ===
    private SchematicReader() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 从文件读取schematic数据
     * 
     * @param filePath 文件路径
     * @return schematic数据，如果文件不存在或读取失败则返回null
     * @throws IOException 文件读取异常
     */
    @Nullable
    public static CompoundTag readSchematic(@Nonnull Path filePath) throws IOException {
        if (!filePath.toFile().exists()) {
            return null;
        }
        
        // 使用NbtIo.readCompressed读取压缩的NBT文件
        CompoundTag rootTag = NbtIo.readCompressed(filePath, NbtAccounter.unlimitedHeap());
        // WorldEdit期望的结构: 根内容直接包含"Schematic"标签
        return rootTag.getCompound(SCHEMATIC_KEY);
    }
    
    /**
     * 获取schematic基本信息
     * 
     * @param schematicTag schematic NBT数据
     * @return schematic信息对象
     */
    @Nonnull
    public static SchematicInfo getSchematicInfo(@Nonnull CompoundTag schematicTag) {
        int version = schematicTag.getInt(VERSION_KEY);
        int dataVersion = schematicTag.getInt(DATA_VERSION_KEY);
        int width = schematicTag.getShort(WIDTH_KEY);
        int height = schematicTag.getShort(HEIGHT_KEY);
        int length = schematicTag.getShort(LENGTH_KEY);
        
        String name = null;
        String author = null;
        
        if (schematicTag.contains(METADATA_KEY)) {
            CompoundTag metadataTag = schematicTag.getCompound(METADATA_KEY);
            if (metadataTag.contains(NAME_KEY)) {
                name = metadataTag.getString(NAME_KEY);
            }
            if (metadataTag.contains(AUTHOR_KEY)) {
                author = metadataTag.getString(AUTHOR_KEY);
            }
        }
        
        return new SchematicInfo(version, dataVersion, width, height, length, name, author);
    }
    
    /**
     * 将schematic粘贴到指定位置
     * 
     * @param serverLevel 服务器世界
     * @param schematicTag schematic NBT数据
     * @param targetPos 目标位置
     * @param pasteOptions 粘贴选项
     * @return 粘贴结果
     */
    @Nonnull
    public static PasteResult pasteSchematic(@Nonnull ServerLevel serverLevel,
                                           @Nonnull CompoundTag schematicTag,
                                           @Nonnull BlockPos targetPos,
                                           @Nonnull PasteOptions pasteOptions) {
        try {
            SchematicInfo schematicInfo = getSchematicInfo(schematicTag);
            
            // 检查区域大小限制
            long totalBlocks = (long) schematicInfo.width() * schematicInfo.height() * schematicInfo.length();
            if (totalBlocks > pasteOptions.maxBlocks()) {
                return new PasteResult(false, 0, 0, 
                    String.format("Schematic too large: %d blocks (max: %d)", totalBlocks, pasteOptions.maxBlocks()));
            }
            
            int blocksPlaced = 0;
            int entitiesPlaced = 0;
            
            // 粘贴方块
            if (pasteOptions.includeBlocks()) {
                blocksPlaced = pasteBlocks(serverLevel, schematicTag, targetPos, pasteOptions);
            }
            
            // 粘贴实体
            if (pasteOptions.includeEntities()) {
                entitiesPlaced = pasteEntities(serverLevel, schematicTag, targetPos, pasteOptions);
            }
            
            return new PasteResult(true, blocksPlaced, entitiesPlaced, null);
            
        } catch (Exception e) {
            return new PasteResult(false, 0, 0, "Paste failed: " + e.getMessage());
        }
    }
    
    /**
     * 粘贴方块
     */
    private static int pasteBlocks(@Nonnull ServerLevel serverLevel,
                                 @Nonnull CompoundTag schematicTag,
                                 @Nonnull BlockPos targetPos,
                                 @Nonnull PasteOptions pasteOptions) throws Exception {
        
        if (!schematicTag.contains(BLOCKS_KEY)) {
            return 0;
        }
        
        CompoundTag blocksContainer = schematicTag.getCompound(BLOCKS_KEY);
        
        // 解析调色板
        Map<Integer, BlockState> palette = parsePalette(blocksContainer.getCompound(PALETTE_KEY));
        
        // 解析方块数据
        List<Integer> blockData = decodeVarintArray(blocksContainer.getByteArray(DATA_KEY));
        
        // 获取尺寸
        int width = schematicTag.getShort(WIDTH_KEY);
        int height = schematicTag.getShort(HEIGHT_KEY);
        int length = schematicTag.getShort(LENGTH_KEY);
        
        int blocksPlaced = 0;
        
        // 放置方块
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int dataIndex = x + z * width + y * width * length;
                    if (dataIndex >= blockData.size()) {
                        continue;
                    }
                    
                    Integer paletteIndex = blockData.get(dataIndex);
                    BlockState blockState = palette.get(paletteIndex);
                    
                    if (blockState != null && shouldPlaceBlock(blockState, pasteOptions)) {
                        BlockPos placePos = targetPos.offset(x, y, z);
                        
                        if (serverLevel.setBlock(placePos, blockState, 3)) {
                            blocksPlaced++;
                        }
                    }
                }
            }
        }
        
        // 处理方块实体
        if (blocksContainer.contains(BLOCK_ENTITIES_KEY)) {
            pasteBlockEntities(serverLevel, blocksContainer.getList(BLOCK_ENTITIES_KEY, Tag.TAG_COMPOUND), targetPos);
        }
        
        return blocksPlaced;
    }
    
    /**
     * 粘贴实体
     */
    private static int pasteEntities(@Nonnull ServerLevel serverLevel,
                                   @Nonnull CompoundTag schematicTag,
                                   @Nonnull BlockPos targetPos,
                                   @Nonnull PasteOptions pasteOptions) {
        
        if (!schematicTag.contains(ENTITIES_KEY)) {
            return 0;
        }
        
        ListTag entitiesList = schematicTag.getList(ENTITIES_KEY, Tag.TAG_COMPOUND);
        int entitiesPlaced = 0;
        
        for (Tag entityTag : entitiesList) {
            if (entityTag instanceof CompoundTag entityCompound) {
                try {
                    if (pasteEntity(serverLevel, entityCompound, targetPos)) {
                        entitiesPlaced++;
                    }
                } catch (Exception e) {
                    // 记录错误但继续处理其他实体
                    System.err.println("Failed to paste entity: " + e.getMessage());
                }
            }
        }
        
        return entitiesPlaced;
    }
    
    /**
     * 解析调色板
     */
    @Nonnull
    private static Map<Integer, BlockState> parsePalette(@Nonnull CompoundTag paletteTag) {
        Map<Integer, BlockState> palette = new HashMap<>();
        
        for (String blockStateString : paletteTag.getAllKeys()) {
            int paletteIndex = paletteTag.getInt(blockStateString);
            BlockState blockState = parseBlockState(blockStateString);
            if (blockState != null) {
                palette.put(paletteIndex, blockState);
            }
        }
        
        return palette;
    }
    
    /**
     * 解析方块状态字符串
     */
    @Nullable
    private static BlockState parseBlockState(@Nonnull String blockStateString) {
        try {
            // 解析格式: "minecraft:stone" 或 "minecraft:stone[variant=granite]"
            int bracketIndex = blockStateString.indexOf('[');
            String blockId = bracketIndex == -1 ? blockStateString : blockStateString.substring(0, bracketIndex);
            
            ResourceLocation blockResourceLocation = ResourceLocation.parse(blockId);
            Block block = BuiltInRegistries.BLOCK.get(blockResourceLocation);
        
            BlockState blockState = block.defaultBlockState();
            
            // 解析属性
            if (bracketIndex != -1 && blockStateString.endsWith("]")) {
                String propertiesString = blockStateString.substring(bracketIndex + 1, blockStateString.length() - 1);
                blockState = parseBlockProperties(blockState, propertiesString);
            }
            
            return blockState;
            
        } catch (Exception e) {
            System.err.println("Failed to parse block state: " + blockStateString + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析方块属性
     */
    @Nonnull
    private static BlockState parseBlockProperties(@Nonnull BlockState blockState, @Nonnull String propertiesString) {
        StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
        
        String[] properties = propertiesString.split(",");
        for (String propertyString : properties) {
            String[] keyValue = propertyString.split("=", 2);
            if (keyValue.length == 2) {
                String propertyName = keyValue[0].trim();
                String propertyValue = keyValue[1].trim();
                
                Property<?> property = stateDefinition.getProperty(propertyName);
                if (property != null) {
                    blockState = setPropertyValue(blockState, property, propertyValue, propertyName);
                }
            }
        }
        
        return blockState;
    }
    
    /**
     * 设置属性值
     */
    private static <T extends Comparable<T>> BlockState setPropertyValue(@Nonnull BlockState blockState, 
                                                                       @Nonnull Property<T> property, 
                                                                       @Nonnull String value, 
                                                                       @Nonnull String propertyName) {
        try {
            Optional<T> parsedValue = property.getValue(value);
            if (parsedValue.isPresent()) {
                return blockState.setValue(property, parsedValue.get());
            } else {
                System.err.println("Invalid property value: " + propertyName + "=" + value);
                return blockState;
            }
        } catch (Exception e) {
            System.err.println("Failed to set property " + propertyName + "=" + value + ": " + e.getMessage());
            return blockState;
        }
    }
    
    /**
     * 解码varint数组
     */
    @Nonnull
    private static List<Integer> decodeVarintArray(@Nonnull byte[] data) throws IOException {
        List<Integer> result = new ArrayList<>();
        
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            
            while (dataInputStream.available() > 0) {
                result.add(readVarint(dataInputStream));
            }
        }
        
        return result;
    }
    
    /**
     * 读取varint值
     */
    private static int readVarint(@Nonnull DataInputStream inputStream) throws IOException {
        int result = 0;
        int shift = 0;
        byte currentByte;
        
        do {
            currentByte = inputStream.readByte();
            result |= (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);
        
        return result;
    }
    
    /**
     * 粘贴方块实体
     */
    private static void pasteBlockEntities(@Nonnull ServerLevel serverLevel,
                                         @Nonnull ListTag blockEntitiesList,
                                         @Nonnull BlockPos targetPos) {
        
        for (Tag blockEntityTag : blockEntitiesList) {
            if (blockEntityTag instanceof CompoundTag blockEntityCompound) {
                try {
                    pasteBlockEntity(serverLevel, blockEntityCompound, targetPos);
                } catch (Exception e) {
                    System.err.println("Failed to paste block entity: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 粘贴单个方块实体
     */
    private static void pasteBlockEntity(@Nonnull ServerLevel serverLevel,
                                       @Nonnull CompoundTag blockEntityCompound,
                                       @Nonnull BlockPos targetPos) throws Exception {
        
        if (!blockEntityCompound.contains("Pos") || !blockEntityCompound.contains("Data")) {
            return;
        }
        
        // 获取相对位置
        int[] relativePos = blockEntityCompound.getIntArray("Pos");
        if (relativePos.length != 3) {
            return;
        }
        
        BlockPos absolutePos = targetPos.offset(relativePos[0], relativePos[1], relativePos[2]);
        BlockEntity blockEntity = serverLevel.getBlockEntity(absolutePos);
        
        if (blockEntity != null) {
            CompoundTag blockEntityData = blockEntityCompound.getCompound("Data");
            
            // 更新位置信息
            blockEntityData.putInt("x", absolutePos.getX());
            blockEntityData.putInt("y", absolutePos.getY());
            blockEntityData.putInt("z", absolutePos.getZ());
            
            // 加载数据到方块实体
            blockEntity.loadWithComponents(blockEntityData, serverLevel.registryAccess());
        }
    }
    
    /**
     * 粘贴单个实体
     */
    private static boolean pasteEntity(@Nonnull ServerLevel serverLevel,
                                     @Nonnull CompoundTag entityCompound,
                                     @Nonnull BlockPos targetPos) throws Exception {
        
        if (!entityCompound.contains("Pos") || !entityCompound.contains("Id")) {
            return false;
        }
        
        // 获取实体类型
        String entityId = entityCompound.getString("Id");
        ResourceLocation entityResourceLocation = ResourceLocation.parse(entityId);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityResourceLocation);
        
        // 获取相对位置
        ListTag positionList = entityCompound.getList("Pos", Tag.TAG_DOUBLE);
        if (positionList.size() != 3) {
            return false;
        }
        
        double relativeX = positionList.getDouble(0);
        double relativeY = positionList.getDouble(1);
        double relativeZ = positionList.getDouble(2);
        
        double absoluteX = targetPos.getX() + relativeX;
        double absoluteY = targetPos.getY() + relativeY;
        double absoluteZ = targetPos.getZ() + relativeZ;
        
        // 创建实体
        Entity entity = entityType.create(serverLevel);
        if (entity == null) {
            return false;
        }
        
        // 设置位置
        entity.setPos(absoluteX, absoluteY, absoluteZ);
        
        // 加载实体数据
        if (entityCompound.contains("Data")) {
            CompoundTag entityData = entityCompound.getCompound("Data");
            entity.load(entityData);
        }
        
        // 添加到世界
        return serverLevel.addFreshEntity(entity);
    }
    
    /**
     * 判断是否应该放置方块
     */
    private static boolean shouldPlaceBlock(@Nonnull BlockState blockState, @Nonnull PasteOptions pasteOptions) {
        if (!pasteOptions.includeAir() && blockState.isAir()) {
            return false;
        }
        
        // 可以在这里添加更多过滤逻辑
        return true;
    }
    
    /**
     * Schematic信息记录类
     */
    public record SchematicInfo(
        int version,
        int dataVersion,
        int width,
        int height,
        int length,
        @Nullable String name,
        @Nullable String author
    ) {}
    
    /**
     * 粘贴选项配置类
     */
    public static final class PasteOptions {
        private final boolean includeBlocks;
        private final boolean includeEntities;
        private final boolean includeAir;
        private final int maxBlocks;
        
        private PasteOptions(boolean includeBlocks, boolean includeEntities, boolean includeAir, int maxBlocks) {
            this.includeBlocks = includeBlocks;
            this.includeEntities = includeEntities;
            this.includeAir = includeAir;
            this.maxBlocks = maxBlocks;
        }
        
        public static PasteOptions defaults() {
            return new PasteOptions(true, true, false, 100000);
        }
        
        public static PasteOptions create(boolean includeBlocks, boolean includeEntities, boolean includeAir, int maxBlocks) {
            return new PasteOptions(includeBlocks, includeEntities, includeAir, maxBlocks);
        }
        
        public boolean includeBlocks() { return includeBlocks; }
        public boolean includeEntities() { return includeEntities; }
        public boolean includeAir() { return includeAir; }
        public int maxBlocks() { return maxBlocks; }
    }
    
    /**
     * 粘贴结果记录类
     */
    public record PasteResult(
        boolean success,
        int blocksPlaced,
        int entitiesPlaced,
        @Nullable String error
    ) {}
}
