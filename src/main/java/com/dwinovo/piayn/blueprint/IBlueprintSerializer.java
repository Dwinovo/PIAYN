package com.dwinovo.piayn.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 蓝图处理器接口
 * 支持多种蓝图格式的保存和处理
 */
public interface IBlueprintSerializer {

    //默认保存路径
    public static final Path DEFAULT_BLUEPRINT_DIR = Paths.get(System.getProperty("user.dir"), "config", "piayn", "blueprints");
    
    /**
     * 生成蓝图数据
     * @param level 服务端世界
     * @param pos1 第一个选择点
     * @param pos2 第二个选择点
     * @param player 玩家
     * @return 蓝图格式的NBT数据
     */
    CompoundTag getTagData(ServerLevel level, BlockPos pos1, BlockPos pos2, Player player);
    
    /**
     * 保存蓝图文件
     * @param blueprintData 蓝图NBT数据
     */
    void exportTagToFile(CompoundTag blueprintData);
    
    /**
     * 获取蓝图格式名称
     * @return 格式名称
     */
    String getFormatName();
    
    /**
     * 获取文件扩展名
     * @return 文件扩展名（不包含点）
     */
    String getFileExtension();
}
