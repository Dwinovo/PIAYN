package com.dwinovo.piayn.client.resource;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.util.List;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import com.dwinovo.piayn.client.resource.pojo.PIAYNModelData;
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

public class PIAYNLoader {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, PIAYNModelData> MODEL_DATA = new ConcurrentHashMap<>();
    private static final Path DATA_PATH = Paths.get(System.getProperty("user.dir"), "piayn", "models");

    /**
     * 客户端资源加载入口
     */
    public static void clientResourceEnterPoint() {
        // 遍历资源文件夹的一级目录
        try {
            File resourceDir = DATA_PATH.toFile();
            if (!FileUtil.exist(resourceDir)) {
                LOGGER.warn("Resource directory does not exist: {}", DATA_PATH);
                return;
            }
            // 获取所有子目录
            List<File> subDirs = FileUtil.loopFiles(resourceDir, 1, file -> file.isDirectory());
            for (File subDir : subDirs) {
                try {
                    // 获取当前目录下所有以.main.json结尾的文件
                    List<File> mainJsonFiles = FileUtil.loopFiles(subDir, file -> 
                        file.isFile() && StrUtil.endWith(file.getName(), ".main.json")
                    );
                    for (File mainJsonFile : mainJsonFiles) {
                        Optional<PIAYNModelData> modelData = parseMainJson(mainJsonFile.toPath());
                        modelData.ifPresent(data -> MODEL_DATA.put(data.getModelID(), data));
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process directory: {}", subDir.getPath(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load models", e);
        }
        
    }

    /**
     * 解析main.json文件
     * @param filePath main.json文件路径
     * @return PIAYNModelData对象，如果加载失败或参数不完整则返回空
     */
    public static Optional<PIAYNModelData> parseMainJson(Path filePath) {
        try {
            // 读取JSON文件内容
            String jsonContent = FileUtil.readUtf8String(filePath.toFile());
            JsonObject jsonObject = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, jsonContent, JsonObject.class);
            
            // 解析基本信息
            String modelName = GsonHelper.getAsString(jsonObject, "model_name");
            String modelID = GsonHelper.getAsString(jsonObject, "model_id");
            String modelPathStr = GsonHelper.getAsString(jsonObject, "model");
            String animationPathStr = GsonHelper.getAsString(jsonObject, "animation");
            String texturePathStr = GsonHelper.getAsString(jsonObject, "texture");
            
            // 验证基本参数不为空
            if (modelName == null || modelName.trim().isEmpty()) {
                LOGGER.error("Model name is null or empty in: {}", filePath);
                return Optional.empty();
            }
            if (modelID == null || modelID.trim().isEmpty()) {
                LOGGER.error("Model ID is null or empty in: {}", filePath);
                return Optional.empty();
            }
            if (modelPathStr == null || modelPathStr.trim().isEmpty()) {
                LOGGER.error("Model path is null or empty in: {}", filePath);
                return Optional.empty();
            }
            if (animationPathStr == null || animationPathStr.trim().isEmpty()) {
                LOGGER.error("Animation path is null or empty in: {}", filePath);
                return Optional.empty();
            }
            if (texturePathStr == null || texturePathStr.trim().isEmpty()) {
                LOGGER.error("Texture path is null or empty in: {}", filePath);
                return Optional.empty();
            }
            
            // 构建完整路径（相对于main.json文件的目录）
            Path baseDir = filePath.getParent();
            Path modelPath = baseDir.resolve(modelPathStr);
            Path animationPath = baseDir.resolve(animationPathStr);
            Path texturePath = baseDir.resolve(texturePathStr);

            LOGGER.debug("Processing main.json file: {}", modelPath);
            LOGGER.debug("Processing animation.json file: {}", animationPath);
            LOGGER.debug("Processing texture.png file: {}", texturePath);
            
            // 加载模型资源
            Optional<BakedGeoModel> model = loadModelFromPath(modelPath);
            Optional<BakedAnimations> animation = loadAnimationFromPath(animationPath);
            Optional<ResourceLocation> texture = loadTextureFromPath(modelID, texturePath);
            
            // 验证所有资源都加载成功
            if (model.isEmpty()) {
                LOGGER.error("Failed to load model from: {}", modelPath);
                return Optional.empty();
            }
            if (animation.isEmpty()) {
                LOGGER.error("Failed to load animation from: {}", animationPath);
                return Optional.empty();
            }
            if (texture.isEmpty()) {
                LOGGER.error("Failed to load texture from: {}", texturePath);
                return Optional.empty();
            }
            
            // 创建并返回PIAYNModelData对象
            PIAYNModelData modelData = new PIAYNModelData();
            modelData.setModelName(modelName.trim());
            modelData.setModelID(modelID.trim());
            modelData.setModel(model.get());
            modelData.setAnimation(animation.get());
            modelData.setTexture(texture.get());
            
            LOGGER.info("Successfully loaded model data: {} ({})", modelName, modelID);
            return Optional.of(modelData);
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse main.json from path: {}", filePath, e);
            return Optional.empty();
        }
    }


    /**
     * 从路径加载模型文件并转换为BakedGeoModel
     * @param filePath 模型文件的绝对路径
     * @return Optional<BakedGeoModel>
     */
    public static Optional<BakedGeoModel> loadModelFromPath(Path filePath) {
        try {
            // 读取文件内容
            String jsonContent = FileUtil.readUtf8String(filePath.toFile());
            Model rawModel = KeyFramesAdapter.GEO_GSON.fromJson(jsonContent, Model.class);
            // 转换为GeometryTree
            GeometryTree geometryTree = GeometryTree.fromModel(rawModel);
            // 使用默认工厂烘焙模型
            BakedGeoModel bakedModel = BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(geometryTree);
            LOGGER.debug("Successfully loaded model from path: {}", filePath);
            return Optional.of(bakedModel);
        } catch (Exception e) {
            LOGGER.error("Failed to load model from path: {}", filePath, e);
            return Optional.empty();
        }
    }
    /**
     * 从路径加载动画文件并转换为BakedAnimations
     * @param filePath 动画文件的绝对路径
     * @return Optional<BakedAnimations>
     */
    public static Optional<BakedAnimations> loadAnimationFromPath(Path filePath) {
        try {
            String jsonContent = FileUtil.readUtf8String(filePath.toFile());
            JsonObject jsonObject = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, jsonContent, JsonObject.class);
            JsonObject animationsObject = GsonHelper.getAsJsonObject(jsonObject, "animations");
            BakedAnimations bakedAnimations = KeyFramesAdapter.GEO_GSON.fromJson(animationsObject, BakedAnimations.class);
            LOGGER.debug("Successfully loaded animation from path: {}", filePath);
            return Optional.of(bakedAnimations);
        } catch (Exception e) {
            LOGGER.error("Failed to load animation from path: {}", filePath, e);
            return Optional.empty();
        }
    }
    /**
     * 从任意路径加载纹理文件并注册到TextureManager
     * @param model_id 模型ID
     * @param filePath 纹理文件的绝对路径
     * @return Optional<ResourceLocation>
     */
    public static Optional<ResourceLocation> loadTextureFromPath(String model_id, Path filePath) {
        try (InputStream stream = FileUtil.getInputStream(filePath.toFile())) {
            NativeImage nativeImage = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(nativeImage);
            ResourceLocation resourceLocation = Minecraft.getInstance().getTextureManager().register(model_id, texture);
            LOGGER.debug("Successfully loaded texture '{}' from path: {} -> {}", model_id, filePath, resourceLocation);
            return Optional.of(resourceLocation);
        } catch (IOException e) {
            LOGGER.error("Failed to load texture '{}' from path: {}", model_id, filePath, e);
            return Optional.empty();
        }
    }
    /**
     * 获取所有模型的id
     * @return Set<String>
     */
    public static Set<String> getAllModelIds() {
        return MODEL_DATA.keySet();
    }
    /**
     * 随机返回一个模型ID
     * @return String
     */
    public static String getRandomModelId() {
        return MODEL_DATA.keySet().stream().collect(Collectors.toList()).get(new Random(System.currentTimeMillis()).nextInt(MODEL_DATA.size()));
    }
    /**
     * 根据id获取模型数据
     * @param modelId 模型ID
     * @return Optional<PIAYNModelData>
     */
    public static Optional<PIAYNModelData> getModelDataById(String modelId) {
        return Optional.ofNullable(MODEL_DATA.get(modelId));
    }
    
}
