package com.dwinovo.piayn.schem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

public class SchemUtils {
    

    public static CompoundTag buildBlockContainer(@Nonnull ServerLevel serverLevel, @Nonnull BlockPos originPos, int width, int height, int length) {
        
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
        blocksContainer.put(SchemSerializer.PALETTE_KEY, paletteTag);
        blocksContainer.put(SchemSerializer.DATA_KEY, encodeVarintArray(blockData));
        
        if (!blockEntitiesList.isEmpty()) {
            ListTag blockEntitiesListTag = new ListTag();
            blockEntitiesList.forEach(blockEntitiesListTag::add);
            blocksContainer.put(SchemSerializer.BLOCK_ENTITIES_KEY, blockEntitiesListTag);
        }
        
        return blocksContainer;
    }
    public static ListTag buildEntitiesList(@Nonnull ServerLevel serverLevel, 
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
     * 创建实体NBT标签
     */
    @Nullable
    public static CompoundTag createEntityTag(@Nonnull Entity entity, @Nonnull BlockPos originPos) {
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
     * 创建方块实体NBT标签
     */
    @Nullable
    public static CompoundTag createBlockEntityTag(@Nonnull ServerLevel serverLevel, @Nonnull BlockEntity blockEntity, int x, int y, int z) {
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
    @Nonnull
    private static String getBlockEntityId(@Nonnull BlockEntity blockEntity) {
        ResourceLocation blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return blockEntityType != null ? blockEntityType.toString() : "unknown";
    }

    /**
     * 将方块状态转换为字符串表示
     */
    @Nonnull
    public static String blockStateToString(@Nonnull BlockState blockState) {
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
    @Nonnull
    public static ByteArrayTag encodeVarintArray(@Nonnull List<Integer> data) {
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
     * 粘贴方块
     */
    public static int pasteBlocks(@Nonnull ServerLevel serverLevel,
                                 @Nonnull CompoundTag schematicTag,
                                 @Nonnull BlockPos targetPos) throws Exception {
        
        if (!schematicTag.contains(SchemSerializer.BLOCKS_KEY)) {
            return 0;
        }
        
        CompoundTag blocksContainer = schematicTag.getCompound(SchemSerializer.BLOCKS_KEY);
        
        // 解析调色板
        Map<Integer, BlockState> palette = parsePalette(blocksContainer.getCompound(SchemSerializer.PALETTE_KEY));
        
        // 解析方块数据
        List<Integer> blockData = decodeVarintArray(blocksContainer.getByteArray(SchemSerializer.DATA_KEY));
        
        // 获取尺寸
        int width = schematicTag.getShort(SchemSerializer.WIDTH_KEY);
        int height = schematicTag.getShort(SchemSerializer.HEIGHT_KEY);
        int length = schematicTag.getShort(SchemSerializer.LENGTH_KEY);
        
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
                    // 跳过空气方块
                    if (blockState != null && !blockState.isAir()) {
                        BlockPos placePos = targetPos.offset(x, y, z);
                        
                        if (serverLevel.setBlock(placePos, blockState, 3)) {
                            blocksPlaced++;
                        }
                    }
                }
            }
        }
        
        // 处理方块实体
        if (blocksContainer.contains(SchemSerializer.BLOCK_ENTITIES_KEY)) {
            pasteBlockEntities(serverLevel, blocksContainer.getList(SchemSerializer.BLOCK_ENTITIES_KEY, Tag.TAG_COMPOUND), targetPos);
        }
        
        return blocksPlaced;
    }
    /**
     * 粘贴实体
     */
    public static int pasteEntities(@Nonnull ServerLevel serverLevel,
                                   @Nonnull CompoundTag schematicTag,
                                   @Nonnull BlockPos targetPos) {
        
        if (!schematicTag.contains(SchemSerializer.ENTITIES_KEY)) {
            return 0;
        }
        
        ListTag entitiesList = schematicTag.getList(SchemSerializer.ENTITIES_KEY, Tag.TAG_COMPOUND);
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
    public static Map<Integer, BlockState> parsePalette(@Nonnull CompoundTag paletteTag) {
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
     * 解析方块状态字符串 - 使用 Minecraft 原生 API
     */
    @Nullable
    public static BlockState parseBlockState(@Nonnull String blockStateString) {
        try {
            // 使用 Minecraft 原生的 BlockStateParser 来解析方块状态字符串
            // 这比手动解析字符串更可靠且兼容性更好
            com.mojang.brigadier.StringReader reader = new com.mojang.brigadier.StringReader(blockStateString);
            net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult result = 
                net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(
                    BuiltInRegistries.BLOCK.asLookup(), reader, false);
            
            return result.blockState();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    /**
     * 解码varint数组
     */
    @Nonnull
    public static List<Integer> decodeVarintArray(@Nonnull byte[] data) throws IOException {
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
    public static int readVarint(@Nonnull DataInputStream inputStream) throws IOException {
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
    public static void pasteBlockEntities(@Nonnull ServerLevel serverLevel,
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
    public static void pasteBlockEntity(@Nonnull ServerLevel serverLevel,
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
    public static boolean pasteEntity(@Nonnull ServerLevel serverLevel,
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

    
}
