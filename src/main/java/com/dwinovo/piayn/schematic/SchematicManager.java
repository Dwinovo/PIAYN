package com.dwinovo.piayn.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Schematic文件管理器
 * 提供统一的schematic文件操作接口
 * 
 * 功能：
 * - 统一管理schematic文件的保存、加载、粘贴操作
 * - 提供标准化的文件路径管理
 * - 封装底层的Writer和Reader操作
 * 
 * @author Dwinovo
 * @version 1.0
 * @since 2025-01-08
 */
public final class SchematicManager {
    
    // === 常量定义 ===
    private static final String FILE_EXTENSION = ".schem";
    private static final String DEFAULT_SCHEMATIC_NAME = "test";
    private static final int MAX_REGION_VOLUME = 100000;
    
    // === 私有构造函数，工具类不允许实例化 ===
    private SchematicManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 获取schematics文件夹路径 - 使用固定路径
     * 
     * @param serverLevel 服务器世界（用于保持接口一致性）
     * @return schematics文件夹路径
     */
    @Nonnull
    private static Path getSchematicsFolder(@Nonnull ServerLevel serverLevel) {
        try {
            // 使用固定路径：run/config/piayn/schematic
            Path schematicsPath = Paths.get(System.getProperty("user.dir"), "config", "piayn", "schematic");
            
            if (!Files.exists(schematicsPath)) {
                Files.createDirectories(schematicsPath);
            }
            
            return schematicsPath;
        } catch (IOException e) {
            throw new RuntimeException("无法创建schematics文件夹: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查区域大小是否合法
     * 
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @return 区域体积
     * @throws IllegalArgumentException 如果区域过大
     */
    private static long validateRegionSize(@Nonnull BlockPos startPos, @Nonnull BlockPos endPos) {
        int width = Math.abs(endPos.getX() - startPos.getX()) + 1;
        int height = Math.abs(endPos.getY() - startPos.getY()) + 1;
        int length = Math.abs(endPos.getZ() - startPos.getZ()) + 1;
        
        long volume = (long) width * height * length;
        if (volume > MAX_REGION_VOLUME) {
            throw new IllegalArgumentException(
                String.format("区域过大: %d 方块 (最大: %d)", volume, MAX_REGION_VOLUME)
            );
        }
        
        return volume;
    }
    
    /**
     * 保存schematic到文件
     * 
     * @param serverLevel 服务器世界
     * @param startPos 起始位置
     * @param endPos 结束位置
     * @param schematicName schematic名称
     * @param authorName 作者名称
     * @return 保存结果
     */
    @Nonnull
    public static SaveResult saveSchematic(@Nonnull ServerLevel serverLevel, 
                                         @Nonnull BlockPos startPos, 
                                         @Nonnull BlockPos endPos, 
                                         @Nullable String schematicName, 
                                         @Nullable String authorName) {
        try {
            // 验证区域大小
            long regionVolume = validateRegionSize(startPos, endPos);
            
            Path schematicsFolder = getSchematicsFolder(serverLevel);
            // 使用固定文件名，方便测试
            String fileName = DEFAULT_SCHEMATIC_NAME + FILE_EXTENSION;
            Path filePath = schematicsFolder.resolve(fileName);
            
            // 保存schematic
            SchematicWriter.writeSchematic(serverLevel, startPos, endPos, filePath, schematicName, authorName);
            return new SaveResult(true, filePath, regionVolume, null);
            
        } catch (Exception e) {
            return new SaveResult(false, null, 0, "保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文件加载schematic
     * 
     * @param serverLevel 服务器世界
     * @param fileName 文件名
     * @return 加载结果
     */
    @Nonnull
    public static LoadResult loadSchematic(@Nonnull ServerLevel serverLevel, @Nonnull String fileName) {
        try {
            Path schematicsFolder = getSchematicsFolder(serverLevel);
            Path filePath = schematicsFolder.resolve(fileName);
            
            if (!fileName.endsWith(FILE_EXTENSION)) {
                filePath = schematicsFolder.resolve(fileName + FILE_EXTENSION);
            }
            
            if (!Files.exists(filePath)) {
                return new LoadResult(false, null, null, "文件不存在: " + filePath.toAbsolutePath());
            }
            
            CompoundTag schematicTag = SchematicReader.readSchematic(filePath);
            if (schematicTag == null) {
                return new LoadResult(false, null, null, "无法解析schematic文件: " + filePath.toAbsolutePath());
            }
            
            SchematicReader.SchematicInfo schematicInfo = SchematicReader.getSchematicInfo(schematicTag);
            return new LoadResult(true, schematicTag, schematicInfo, null);
            
        } catch (Exception e) {
            return new LoadResult(false, null, null, "加载失败: " + e.getMessage() + " - " + e.getClass().getSimpleName());
        }
    }
    
    /**
     * 粘贴schematic到指定位置
     * 
     * @param serverLevel 服务器世界
     * @param schematicTag schematic数据
     * @param targetPos 目标位置
     * @param pasteOptions 粘贴选项
     * @return 粘贴结果
     */
    @Nonnull
    public static PasteResult pasteSchematic(@Nonnull ServerLevel serverLevel,
                                           @Nonnull CompoundTag schematicTag,
                                           @Nonnull BlockPos targetPos,
                                           @Nonnull SchematicReader.PasteOptions pasteOptions) {
        try {
            SchematicReader.PasteResult readerResult = SchematicReader.pasteSchematic(
                serverLevel, schematicTag, targetPos, pasteOptions
            );
            
            return new PasteResult(
                readerResult.success(),
                readerResult.blocksPlaced(),
                readerResult.entitiesPlaced(),
                readerResult.error()
            );
            
        } catch (Exception e) {
            return new PasteResult(false, 0, 0, "粘贴失败: " + e.getMessage());
        }
    }
    
    /**
     * 列出所有schematic文件
     * 
     * @param serverLevel 服务器世界
     * @return schematic文件名列表
     */
    @Nonnull
    public static List<String> listSchematicFiles(@Nonnull ServerLevel serverLevel) {
        try {
            Path schematicsFolder = getSchematicsFolder(serverLevel);
            return Files.list(schematicsFolder)
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .map(path -> path.getFileName().toString())
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 列出所有schematic文件（返回数组格式，兼容旧接口）
     * 
     * @param serverLevel 服务器世界
     * @return schematic文件名数组
     */
    @Nonnull
    public static String[] listSchematics(@Nonnull ServerLevel serverLevel) {
        List<String> schematicsList = listSchematicFiles(serverLevel);
        return schematicsList.toArray(new String[0]);
    }
    

    
    // === 结果类定义 ===
    
    /**
     * 保存结果记录类
     * 
     * @param success 是否成功
     * @param filePath 文件路径
     * @param regionVolume 区域体积
     * @param error 错误信息
     */
    public record SaveResult(
        boolean success,
        @Nullable Path filePath,
        long regionVolume,
        @Nullable String error
    ) {
        @Override
        public String toString() {
            if (success) {
                return String.format("SaveResult{success=true, file='%s', volume=%d}", 
                    filePath != null ? filePath.getFileName() : "null", regionVolume);
            } else {
                return String.format("SaveResult{success=false, error='%s'}", error);
            }
        }
    }
    
    /**
     * 加载结果记录类
     * 
     * @param success 是否成功
     * @param schematicTag schematic数据
     * @param schematicInfo schematic信息
     * @param error 错误信息
     */
    public record LoadResult(
        boolean success,
        @Nullable CompoundTag schematicTag,
        @Nullable SchematicReader.SchematicInfo schematicInfo,
        @Nullable String error
    ) {
        @Override
        public String toString() {
            if (success && schematicInfo != null) {
                return String.format("LoadResult{success=true, size=%dx%dx%d, name='%s'}", 
                    schematicInfo.width(), schematicInfo.height(), schematicInfo.length(),
                    schematicInfo.name() != null ? schematicInfo.name() : "unnamed");
            } else {
                return String.format("LoadResult{success=false, error='%s'}", error);
            }
        }
    }
    
    /**
     * 粘贴结果记录类
     * 
     * @param success 是否成功
     * @param blocksPlaced 放置的方块数量
     * @param entitiesPlaced 放置的实体数量
     * @param error 错误信息
     */
    public record PasteResult(
        boolean success,
        int blocksPlaced,
        int entitiesPlaced,
        @Nullable String error
    ) {
        @Override
        public String toString() {
            if (success) {
                return String.format("PasteResult{success=true, blocks=%d, entities=%d}", 
                    blocksPlaced, entitiesPlaced);
            } else {
                return String.format("PasteResult{success=false, error='%s'}", error);
            }
        }
    }
}
