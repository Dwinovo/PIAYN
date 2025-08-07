package com.dwinovo.piayn.blueprint.impl;

import com.dwinovo.piayn.blueprint.IBlueprintSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schematic格式蓝图处理器
 * 实现标准的.schematic文件格式保存
 */
public class SchematicSerialize implements IBlueprintSerializer {
    
    @Override
    public CompoundTag getTagData(ServerLevel level, BlockPos pos1, BlockPos pos2, Player player) {
        // 计算区域边界
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;
        
        // 创建schematic NBT结构
        CompoundTag schematicTag = new CompoundTag();
        schematicTag.putShort("Width", (short) width);
        schematicTag.putShort("Height", (short) height);
        schematicTag.putShort("Length", (short) length);
        schematicTag.putString("Materials", "Alpha");
        
        // 收集方块数据
        byte[] blocks = new byte[width * height * length];
        byte[] blockData = new byte[width * height * length];
        List<CompoundTag> tileEntities = new ArrayList<>();
        Map<Block, Integer> blockIdMap = new HashMap<>();
        
        // 构建方块ID映射
        int nextBlockId = 0;
        blockIdMap.put(Blocks.AIR, nextBlockId++);
        
        // 遍历区域内的所有方块
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos currentPos = new BlockPos(minX + x, minY + y, minZ + z);
                    BlockState blockState = level.getBlockState(currentPos);
                    Block block = blockState.getBlock();
                    
                    // 获取或分配方块ID
                    if (!blockIdMap.containsKey(block)) {
                        blockIdMap.put(block, nextBlockId++);
                    }
                    
                    int index = (y * length + z) * width + x; // YZX顺序
                    blocks[index] = (byte) (int) blockIdMap.get(block);
                    
                    // 获取方块数据（暂时设为0，现代版本使用方块状态）
                    blockData[index] = 0;
                    
                    // 处理方块实体
                    BlockEntity blockEntity = level.getBlockEntity(currentPos);
                    if (blockEntity != null) {
                        CompoundTag tileEntityTag = blockEntity.saveWithoutMetadata(level.registryAccess());
                        // 调整坐标为相对坐标
                        tileEntityTag.putInt("x", x);
                        tileEntityTag.putInt("y", y);
                        tileEntityTag.putInt("z", z);
                        tileEntities.add(tileEntityTag);
                    }
                }
            }
        }
        
        // 添加数据到NBT
        schematicTag.putByteArray("Blocks", blocks);
        schematicTag.putByteArray("Data", blockData);
        
        // 添加方块实体
        ListTag tileEntitiesTag = new ListTag();
        for (CompoundTag tileEntity : tileEntities) {
            tileEntitiesTag.add(tileEntity);
        }
        schematicTag.put("TileEntities", tileEntitiesTag);
        
        // 添加空的实体列表
        schematicTag.put("Entities", new ListTag());
        
        return schematicTag;
    }
    
    @Override
    public void exportTagToFile(CompoundTag blueprintData) {
        try {
            String fileName = "blueprint_" + System.currentTimeMillis() + "." + getFileExtension();
            FileOutputStream fos = new FileOutputStream(IBlueprintSerializer.DEFAULT_BLUEPRINT_DIR.resolve(fileName).toFile());
            NbtIo.writeCompressed(blueprintData, fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String getFormatName() {
        return "Schematic";
    }
    
    @Override
    public String getFileExtension() {
        return "schematic";
    }
}
