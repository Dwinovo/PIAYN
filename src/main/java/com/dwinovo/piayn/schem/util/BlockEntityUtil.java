package com.dwinovo.piayn.schem.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * 方块实体处理工具类
 * 处理方块实体的创建、解析和粘贴操作
 */
public class BlockEntityUtil {
    
    /**
     * 创建方块实体NBT标签
     */
    @Nullable
    public static CompoundTag createBlockEntityTag(@Nonnull Level level, 
                                                  @Nonnull BlockEntity blockEntity, 
                                                  int x, int y, int z) {
        try {
            CompoundTag blockEntityTag = new CompoundTag();
            
            // 获取方块实体类型ID
            String blockEntityId = getBlockEntityId(blockEntity);
            if (blockEntityId == null) {
                return null;
            }
            
            blockEntityTag.putString("Id", blockEntityId);
            blockEntityTag.putIntArray("Pos", new int[]{x, y, z});
            
            // 保存方块实体数据
            CompoundTag blockEntityData = blockEntity.saveWithoutMetadata(level.registryAccess());
            
            // 移除位置信息，因为我们使用相对位置
            blockEntityData.remove("x");
            blockEntityData.remove("y");
            blockEntityData.remove("z");
            
            if (!blockEntityData.isEmpty()) {
                blockEntityTag.put("Data", blockEntityData);
            }
            
            return blockEntityTag;
            
        } catch (Exception e) {
            System.err.println("Failed to create block entity tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取方块实体类型ID
     */
    @Nullable
    public static String getBlockEntityId(@Nonnull BlockEntity blockEntity) {
        ResourceLocation blockEntityId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return blockEntityId != null ? blockEntityId.toString() : null;
    }
    
    /**
     * 粘贴方块实体到世界中
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
    
    private BlockEntityUtil() {
        // 工具类，禁止实例化
    }
}
