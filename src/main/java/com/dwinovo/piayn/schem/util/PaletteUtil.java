package com.dwinovo.piayn.schem.util;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 调色板处理工具类
 * 参考 WorldEdit 的 PaletteMap 设计
 */
public class PaletteUtil {
    
    /**
     * 调色板映射类 - 管理方块状态到索引的映射
     */
    public static class PaletteMap {
        private final Map<BlockState, Integer> blockStateToPaletteIndex = new LinkedHashMap<>();
        private int nextId = 0;
        
        /**
         * 获取方块状态的调色板索引，如果不存在则创建新的
         */
        public int getId(@Nonnull BlockState blockState) {
            Integer existingId = blockStateToPaletteIndex.get(blockState);
            if (existingId != null) {
                return existingId;
            }
            
            int newId = nextId++;
            blockStateToPaletteIndex.put(blockState, newId);
            return newId;
        }
        
        /**
         * 转换为 NBT 格式的调色板
         */
        @Nonnull
        public CompoundTag toNbt() {
            CompoundTag paletteTag = new CompoundTag();
            for (Map.Entry<BlockState, Integer> entry : blockStateToPaletteIndex.entrySet()) {
                String blockStateString = BlockUtil.blockStateToString(entry.getKey());
                paletteTag.putInt(blockStateString, entry.getValue());
            }
            return paletteTag;
        }
        
        /**
         * 获取调色板大小
         */
        public int size() {
            return blockStateToPaletteIndex.size();
        }
    }
    
    /**
     * 解析 NBT 调色板为方块状态映射
     */
    @Nonnull
    public static Map<Integer, BlockState> parsePalette(@Nonnull CompoundTag paletteTag) {
        Map<Integer, BlockState> palette = new LinkedHashMap<>();
        
        for (String blockStateString : paletteTag.getAllKeys()) {
            int paletteIndex = paletteTag.getInt(blockStateString);
            BlockState blockState = BlockUtil.parseBlockState(blockStateString);
            if (blockState != null) {
                palette.put(paletteIndex, blockState);
            }
        }
        
        return palette;
    }
    
    private PaletteUtil() {
        // 工具类，禁止实例化
    }
}
