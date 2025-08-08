package com.dwinovo.piayn.schem.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.dwinovo.piayn.schem.SchemSerializer;

/**
 * 方块容器构建工具类
 * 负责构建 Sponge Schematic 格式的方块容器
 */
public class BlockContainerUtil {
    
    /**
     * 构建方块容器
     */
    @Nonnull
    public static CompoundTag buildBlockContainer(@Nonnull ServerLevel serverLevel, 
                                                 @Nonnull BlockPos originPos, 
                                                 int width, int height, int length) {
        
        // 调色板：方块状态 -> 索引
        PaletteUtil.PaletteMap paletteMap = new PaletteUtil.PaletteMap();
        List<Integer> blockData = new ArrayList<>();
        List<CompoundTag> blockEntitiesList = new ArrayList<>();
        
        // 遍历区域内所有方块
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos currentPos = originPos.offset(x, y, z);
                    BlockState blockState = serverLevel.getBlockState(currentPos);
                    
                    // 获取或创建调色板索引
                    int paletteIndex = paletteMap.getId(blockState);
                    blockData.add(paletteIndex);
                    
                    // 处理方块实体
                    BlockEntity blockEntity = serverLevel.getBlockEntity(currentPos);
                    if (blockEntity != null) {
                        CompoundTag blockEntityTag = BlockEntityUtil.createBlockEntityTag(serverLevel, blockEntity, x, y, z);
                        if (blockEntityTag != null) {
                            blockEntitiesList.add(blockEntityTag);
                        }
                    }
                }
            }
        }
        
        // 构建方块容器
        CompoundTag blocksContainer = new CompoundTag();
        blocksContainer.put(SchemSerializer.PALETTE_KEY, paletteMap.toNbt());
        blocksContainer.put(SchemSerializer.DATA_KEY, VarIntUtil.encodeVarintArray(blockData));
        
        if (!blockEntitiesList.isEmpty()) {
            ListTag blockEntitiesListTag = new ListTag();
            blockEntitiesList.forEach(blockEntitiesListTag::add);
            blocksContainer.put(SchemSerializer.BLOCK_ENTITIES_KEY, blockEntitiesListTag);
        }
        
        return blocksContainer;
    }
    
    private BlockContainerUtil() {
        // 工具类，禁止实例化
    }
}
