package com.dwinovo.piayn.client.resource.schematic.cache;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.world.schematic.io.NbtStructureIO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SchematicTemplateCache {
    // 单例实例
    private static final SchematicTemplateCache INSTANCE = new SchematicTemplateCache();
    
    // 使用 Guava CacheBuilder 创建缓存：按文件名缓存 StructureTemplate
    private final Cache<String, StructureTemplate> cache;
    
    private SchematicTemplateCache() {
        // 配置：最多缓存 100 个模板，5 分钟未访问即过期
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
        
    }
    
    /**
     * 获取单例实例
     */
    public static SchematicTemplateCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取或加载指定蓝图文件名对应的 StructureTemplate。
     * 若缓存中不存在则从磁盘加载并放入缓存。
     * @param schematicFileName 蓝图文件名（可不含 .nbt 后缀）
     * @return 对应的 StructureTemplate；失败返回 null
     */
    public StructureTemplate getOrLoadTemplate(Level level, String schematicFileName) {
        if (schematicFileName == null || schematicFileName.isEmpty()) {
            return null;
        }
        
        try {
            // 若不存在则自动加载
            return cache.get(schematicFileName, () -> createNewTemplate(level, schematicFileName));
        } catch (Exception e) {
            PIAYN.LOGGER.error("获取或加载蓝图模板失败: {}", schematicFileName, e);
            return null;
        }
    }
    
    /**
     * 实际从磁盘加载 .nbt 为 StructureTemplate。
     */
    private StructureTemplate createNewTemplate(Level level, String schematicFileName) {
        try {
            return NbtStructureIO.loadNbtToStructureTemplate(level, schematicFileName);
        } catch (IOException e) {
            PIAYN.LOGGER.error("加载蓝图模板失败: {}", schematicFileName, e);
            return null;
        }
    }
    
    /**
     * 手动移除某个文件名对应的缓存模板
     * @param schematicFileName 蓝图文件名
     */
    public void removeFromCache(String schematicFileName) {
        cache.invalidate(schematicFileName);
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cache.invalidateAll();
    }
    
    /**
     * 获取当前缓存大小
     */
    public long getCacheSize() {
        return cache.size();
    }
    
    /**
     * 获取缓存统计信息（需要启用统计功能；当前未启用，返回空统计）
     */
    public String getCacheStats() {
        return cache.stats().toString();
    }
}
