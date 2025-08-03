package com.dwinovo.piayn.client.resource;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

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
import software.bernie.geckolib.loading.FileLoader;

public class ResourceManager {
    public static final Logger LOGGER = LogUtils.getLogger();
    // 自定义模型缓存，使用字符串ID而非ResourceLocation
    private static final Map<String, BakedGeoModel> CUSTOM_MODELS = new ConcurrentHashMap<>();
    private static final Map<String, BakedAnimations> CUSTOM_ANIMATIONS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> CUSTOM_TEXTURES = new ConcurrentHashMap<>();


    
    /**
     * 从任意路径加载模型文件并转换为BakedGeoModel
     * @param modelId 自定义模型ID
     * @param filePath 模型文件的绝对路径
     * @return 是否加载成功
     */
    public static boolean loadModelFromPath(String modelId, String filePath) {
        try {
            // 读取文件内容
            String jsonContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            
            // 使用Geckolib的Gson解析器解析模型
            Model rawModel = KeyFramesAdapter.GEO_GSON.fromJson(jsonContent, Model.class);
            
            // 转换为GeometryTree
            GeometryTree geometryTree = GeometryTree.fromModel(rawModel);
            
            // 使用默认工厂烘焙模型
            BakedGeoModel bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(geometryTree);
            
            // 存储到自定义缓存
            CUSTOM_MODELS.put(modelId, bakedModel);
            // 注意：不能直接修改GeckoLibCache，因为它是不可修改的
            // 我们使用自己的缓存系统来管理自定义模型
            LOGGER.info("Successfully loaded custom model '{}' from path: {}", modelId, filePath);
            
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load model from path: " + filePath);
            e.printStackTrace();
            return false;
        }
    }
    public static boolean loadAnimationFromPath(String animationId, String filePath) {
        try {
            // 1. 直接读取动画文件内容
            String jsonContent = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

            // 2. 使用GeckoLib的专用Gson解析器
            JsonObject jsonObject = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, jsonContent, JsonObject.class);
            
            // 3. 提取animations部分并解析为BakedAnimations
            JsonObject animationsObject = GsonHelper.getAsJsonObject(jsonObject, "animations");
            BakedAnimations bakedAnimations = KeyFramesAdapter.GEO_GSON.fromJson(animationsObject, BakedAnimations.class);
            
            // 4. 缓存动画
            CUSTOM_ANIMATIONS.put(animationId, bakedAnimations);
            
            LOGGER.info("Successfully loaded custom animation '{}' from path: {}", animationId, filePath);
            
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load animation from path: " + filePath);
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 从任意路径加载纹理文件并注册到TextureManager
     * @param textureId 自定义纹理ID
     * @param filePath 纹理文件的绝对路径
     * @return 是否加载成功
     */
    public static boolean loadTextureFromPath(String textureId, String filePath) {
        try (InputStream stream = Files.newInputStream(Paths.get(filePath))) {
            // 使用textureId作为注册名称，确保唯一性
            String registerName = textureId;
            
            NativeImage nativeImage = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(nativeImage);
            
            // 注册到TextureManager，会自动生成唯一的ResourceLocation
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register(registerName, texture);
            
            // 存储到自定义缓存
            CUSTOM_TEXTURES.put(textureId, location);
            
            LOGGER.info("Successfully loaded texture '{}' from path: {} -> {}", textureId, filePath, location);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to load texture '{}' from path: {}", textureId, filePath, e);
            return false;
        }
    }
    
    
    /**
     * 获取自定义纹理
     */
    public static ResourceLocation getCustomTexture(String textureId) {
        return CUSTOM_TEXTURES.get(textureId);
    }
    /**
     * 获取自定义模型
     */
    public static BakedGeoModel getCustomModel(String modelId) {
        return CUSTOM_MODELS.get(modelId);
    }
    
    public static BakedAnimations getCustomAnimation(String animationId) {
        return CUSTOM_ANIMATIONS.get(animationId);
    }
    
    /**
     * 删除模型
     */
    public static boolean removeModel(String modelId) {

        return CUSTOM_MODELS.remove(modelId) != null;
    }

    
    /**
     * 获取所有已加载的模型ID
     */
    public static Set<String> getAllModelIds() {
        return new HashSet<>(CUSTOM_MODELS.keySet());
    }
    
    /**
     * 检查模型是否存在
     */
    public static boolean hasModel(String modelId) {
        return CUSTOM_MODELS.containsKey(modelId);
    }
}
