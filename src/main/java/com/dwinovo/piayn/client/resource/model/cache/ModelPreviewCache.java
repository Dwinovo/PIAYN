package com.dwinovo.piayn.client.resource.model.cache;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.concurrent.TimeUnit;

/**
 * 实体预览缓存管理器
 * 使用Google Guava的CacheBuilder实现基于ModelID的宠物实体缓存
 * 避免频繁创建实体造成性能问题
 */
@OnlyIn(Dist.CLIENT)
public class ModelPreviewCache {
    
    // 单例实例
    private static final ModelPreviewCache INSTANCE = new ModelPreviewCache();
    
    // 使用Guava CacheBuilder创建缓存
    private final Cache<String, PetEntity> cache;
    
    private ModelPreviewCache() {
        // 使用Guava CacheBuilder配置缓存
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(100)  // 最大缓存100个实体
                .expireAfterAccess(5, TimeUnit.MINUTES)  // 5分钟未访问后过期
                .removalListener((RemovalListener<String, PetEntity>) notification -> {
                    // 缓存项被移除时的回调
                })
                .build();
        
    }
    
    /**
     * 获取单例实例
     */
    public static ModelPreviewCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * 根据ModelID获取缓存的实体，如果不存在则创建新的
     * @param modelId 模型ID
     * @return 宠物实体，如果创建失败返回null
     */
    public PetEntity getOrCreateEntity(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return null;
        }
        
        try {
            // 使用Guava Cache的get方法，如果不存在则自动创建
            return cache.get(modelId, () -> {
                PetEntity entity = createNewEntity(modelId);
                return entity;
            });
        } catch (Exception e) {
            PIAYN.LOGGER.error("获取或创建实体预览时发生错误: {}", modelId, e);
            return null;
        }
    }
    
    /**
     * 创建新的宠物实体
     * @param modelId 模型ID
     * @return 新创建的宠物实体，如果创建失败返回null
     */
    private PetEntity createNewEntity(String modelId) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            PIAYN.LOGGER.warn("World is null, cannot create preview entity for model: {}", modelId);
            return null;
        }
        
        PetEntity entity = PetEntity.TYPE.create(world);
        if (entity == null) {
            PIAYN.LOGGER.warn("Failed to create preview entity for model: {}", modelId);
            return null;
        }
        
        entity.setModelID(modelId);
        return entity;
    }
    
    /**
     * 手动清理指定ModelID的缓存
     * @param modelId 要清理的模型ID
     */
    public void removeFromCache(String modelId) {
        cache.invalidate(modelId);
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
     * 获取缓存统计信息（需要启用统计功能）
     * 注意：当前配置未启用统计，此方法返回空统计信息
     */
    public String getCacheStats() {
        return cache.stats().toString();
    }
}
