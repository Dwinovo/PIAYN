package com.dwinovo.piayn.client.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.dwinovo.piayn.client.resource.pojo.ClientModelData;
import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;

/**
 * 客户端模型数据管理器
 * 负责将服务端传输的模型数据转换为客户端可用的模型数据并缓存
 * 
 * @author PIAYN Team
 * @since 1.0.0
 */
public class ClientModelDataManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 客户端模型数据缓存
     * Key: 模型ID, Value: 客户端模型数据
     */
    private static final Map<String, ClientModelData> MODEL_DATA = new ConcurrentHashMap<>();
    
    /**
     * 处理服务端模型数据，转换为客户端模型数据并缓存
     * 
     * @param serverModelData 服务端模型数据
     * @return 是否处理成功
     */
    public static boolean processServerModelData(ServerModelData serverModelData) {
        if (serverModelData == null) {
            LOGGER.warn("ServerModelData is null, skipping processing");
            return false;
        }
        
        try {
            String modelId = serverModelData.getModelID();
            String modelName = serverModelData.getModelName();
            
            LOGGER.debug("Processing server model data: {} ({})", modelName, modelId);
            
            // 转换模型数据
            Optional<BakedGeoModel> bakedModel = convertToGeoModel(serverModelData.getModel(), modelId);
            Optional<BakedAnimations> bakedAnimations = convertToAnimations(serverModelData.getAnimation(), modelId);
            Optional<ResourceLocation> textureLocation = convertToTexture(serverModelData.getTexture(), modelId);
            
            // 验证所有转换是否成功
            if (bakedModel.isEmpty() || bakedAnimations.isEmpty() || textureLocation.isEmpty()) {
                LOGGER.error("Failed to convert server model data for: {} ({})", modelName, modelId);
                return false;
            }
            
            // 创建客户端模型数据
            ClientModelData clientModelData = new ClientModelData(
                modelName,
                modelId,
                bakedModel.get(),
                bakedAnimations.get(),
                textureLocation.get()
            );
            
            // 存储到缓存
            MODEL_DATA.put(modelId, clientModelData);
            
            LOGGER.info("Successfully processed and cached model: {} ({})", modelName, modelId);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error processing server model data: {}", serverModelData.getModelID(), e);
            return false;
        }
    }
    
    /**
     * 将字节数组转换为BakedGeoModel
     * 暂时返回null，等待后续实现具体的转换逻辑
     * 
     * @param modelBytes 模型字节数据
     * @param modelId 模型ID
     * @return BakedGeoModel的Optional包装
     */
    private static Optional<BakedGeoModel> convertToGeoModel(byte[] modelBytes, String modelId) {
        try {
            String jsonContent = new String(modelBytes, StandardCharsets.UTF_8);
            Model rawModel = KeyFramesAdapter.GEO_GSON.fromJson(jsonContent, Model.class);
            // 转换为GeometryTree
            GeometryTree geometryTree = GeometryTree.fromModel(rawModel);
            // 使用默认工厂烘焙模型
            BakedGeoModel bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(geometryTree);
            
            LOGGER.debug("Model conversion not yet implemented for: {}", modelId);
            return Optional.of(bakedModel);
            
        } catch (Exception e) {
            LOGGER.error("Error converting geometry for model: {}", modelId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 将字节数组转换为BakedAnimations
     * 暂时返回null，等待后续实现具体的转换逻辑
     * 
     * @param animationBytes 动画字节数据
     * @param modelId 模型ID
     * @return BakedAnimations的Optional包装
     */
    private static Optional<BakedAnimations> convertToAnimations(byte[] animationBytes, String modelId) {
        try {
            String jsonContent = new String(animationBytes, StandardCharsets.UTF_8);
            JsonObject jsonObject = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, jsonContent, JsonObject.class);
            JsonObject animationsObject = GsonHelper.getAsJsonObject(jsonObject, "animations");
            BakedAnimations bakedAnimations = KeyFramesAdapter.GEO_GSON.fromJson(animationsObject, BakedAnimations.class);
            
            LOGGER.debug("Animation conversion not yet implemented for: {}", modelId);
            return Optional.of(bakedAnimations);
            
        } catch (Exception e) {
            LOGGER.error("Error converting animations for model: {}", modelId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 将字节数组转换为纹理ResourceLocation
     * 
     * @param textureBytes 纹理字节数据
     * @param modelId 模型ID
     * @return ResourceLocation的Optional包装
     */
    private static Optional<ResourceLocation> convertToTexture(byte[] textureBytes, String modelId) {
        try {
            // 从字节数组创建NativeImage
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(textureBytes));
            
            // 创建动态纹理
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            
            // 注册到纹理管理器
            String textureId = "piayn_model_" + modelId;
            ResourceLocation textureLocation = Minecraft.getInstance()
                .getTextureManager()
                .register(textureId, dynamicTexture);
            
            LOGGER.debug("Successfully registered texture for model: {} -> {}", modelId, textureLocation);
            return Optional.of(textureLocation);
            
        } catch (IOException e) {
            LOGGER.error("Failed to read texture image for model: {}", modelId, e);
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Unexpected error converting texture for model: {}", modelId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有模型ID
     * 
     * @return 模型ID集合
     */
    public static Set<String> getAllModelIds() {
        return Set.copyOf(MODEL_DATA.keySet());
    }
    
    /**
     * 根据ID获取模型数据
     * 
     * @param modelId 模型ID
     * @return 客户端模型数据的Optional包装
     */
    public static Optional<ClientModelData> getModelDataById(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            LOGGER.warn("Model ID is null or empty");
            return Optional.empty();
        }
        return Optional.ofNullable(MODEL_DATA.get(modelId));
    }
    
    /**
     * 根据ID获取模型名称
     * 
     * @param modelId 模型ID
     * @return 模型名称，如果不存在则返回默认值
     */
    public static String getModelNameById(String modelId) {
        return getModelDataById(modelId)
            .map(ClientModelData::getModelName)
            .orElse("乌萨奇");
    }
    
    /**
     * 随机获取一个模型ID
     * 
     * @return 随机模型ID，如果没有模型则返回默认值
     */
    public static String getRandomModelId() {
        Set<String> modelIds = MODEL_DATA.keySet();
        return modelIds.stream()
            .skip((int) (modelIds.size() * Math.random()))
            .findFirst()
            .orElse("usagi");
    }
    
    /**
     * 检查模型是否存在
     * 
     * @param modelId 模型ID
     * @return 是否存在
     */
    public static boolean hasModel(String modelId) {
        return modelId != null && MODEL_DATA.containsKey(modelId);
    }
    
    /**
     * 获取已缓存的模型数量
     * 
     * @return 模型数量
     */
    public static int getModelCount() {
        return MODEL_DATA.size();
    }
    
    /**
     * 清空所有缓存的模型数据
     */
    public static void clearCache() {
        MODEL_DATA.clear();
        LOGGER.info("Cleared all cached model data");
    }
}
