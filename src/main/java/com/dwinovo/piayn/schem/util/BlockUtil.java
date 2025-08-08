package com.dwinovo.piayn.schem.util;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.dwinovo.piayn.schem.SchemSerializer;

/**
 * 方块处理工具类
 * 处理方块状态解析、转换和粘贴操作
 */
public class BlockUtil {
    
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
    
    /**
     * 粘贴方块到世界中
     */
    public static int pasteBlocks(@Nonnull ServerLevel serverLevel,
                                 @Nonnull CompoundTag schematicTag,
                                 @Nonnull BlockPos targetPos) {
        
        CompoundTag blocksContainer = schematicTag.getCompound(SchemSerializer.BLOCKS_KEY);
        if (blocksContainer.isEmpty()) {
            return 0;
        }
        
        // 解析调色板
        CompoundTag paletteTag = blocksContainer.getCompound(SchemSerializer.PALETTE_KEY);
        Map<Integer, BlockState> palette = PaletteUtil.parsePalette(paletteTag);
        
        // 解码方块数据
        byte[] blockDataBytes = blocksContainer.getByteArray(SchemSerializer.DATA_KEY);
        List<Integer> blockData = VarIntUtil.decodeVarintArray(blockDataBytes);
        
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
            BlockEntityUtil.pasteBlockEntities(serverLevel, 
                blocksContainer.getList(SchemSerializer.BLOCK_ENTITIES_KEY, Tag.TAG_COMPOUND), targetPos);
        }
        
        return blocksPlaced;
    }
    
    private BlockUtil() {
        // 工具类，禁止实例化
    }
}
