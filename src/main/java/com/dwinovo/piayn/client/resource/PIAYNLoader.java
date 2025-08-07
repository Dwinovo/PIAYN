package com.dwinovo.piayn.client.resource;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.function.Function;
import java.io.File;
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
    private static final Path DATA_PATH = Paths.get(System.getProperty("user.dir"), "config", "piayn", "models");

    /**
     * 客户端资源加载入口
     */
    public static void clientResourceEnterPoint() {
        Optional.of(DATA_PATH.toFile())
            .filter(FileUtil::exist)
            .map(PIAYNLoader::getSubDirectories)
            .orElseGet(() -> {
                LOGGER.warn("Resource directory does not exist: {}", DATA_PATH);
                return Stream.empty();
            })
            .forEach(PIAYNLoader::processDirectory);
    }
    
    /**
     * 获取资源目录下的所有子目录
     * @param resourceDir 资源根目录
     * @return 子目录流
     */
    private static Stream<File> getSubDirectories(File resourceDir) {
        try {
            return FileUtil.loopFiles(resourceDir, 1, File::isDirectory).stream();
        } catch (Exception e) {
            LOGGER.error("Failed to scan resource directory: {}", resourceDir.getPath(), e);
            return Stream.empty();
        }
    }
    
    /**
     * 处理单个目录，加载其中的模型数据
     * @param subDir 要处理的子目录
     */
    private static void processDirectory(File subDir) {
        try {
            findMainJsonFiles(subDir)
                .map(File::toPath)
                .map(PIAYNLoader::parseMainJson)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(data -> MODEL_DATA.put(data.getModelID(), data));
        } catch (Exception e) {
            LOGGER.error("Failed to process directory: {}", subDir.getPath(), e);
        }
    }
    
    /**
     * 在指定目录中查找所有.main.json文件
     * @param directory 要搜索的目录
     * @return main.json文件流
     */
    private static Stream<File> findMainJsonFiles(File directory) {
        try {
            return FileUtil.loopFiles(directory, file -> 
                file.isFile() && StrUtil.endWith(file.getName(), ".main.json")
            ).stream();
        } catch (Exception e) {
            LOGGER.error("Failed to find main.json files in directory: {}", directory.getPath(), e);
            return Stream.empty();
        }
    }

    /**
     * 解析main.json文件
     * @param filePath main.json文件路径
     * @return PIAYNModelData对象，如果加载失败或参数不完整则返回空
     */
    public static Optional<PIAYNModelData> parseMainJson(Path filePath) {
        try {
            return readJsonFile(filePath)
                .flatMap(jsonObject -> extractBasicFields(jsonObject, filePath))
                .flatMap(fields -> loadResourcesAndBuild(fields, filePath));
        } catch (Exception e) {
            LOGGER.error("Failed to parse main.json from path: {}", filePath, e);
            return Optional.empty();
        }
    }
    
    /**
     * 读取并解析JSON文件
     */
    private static Optional<JsonObject> readJsonFile(Path filePath) {
        try {
            String jsonContent = FileUtil.readUtf8String(filePath.toFile());
            JsonObject jsonObject = GsonHelper.fromJson(KeyFramesAdapter.GEO_GSON, jsonContent, JsonObject.class);
            return Optional.of(jsonObject);
        } catch (Exception e) {
            LOGGER.error("Failed to read JSON file: {}", filePath, e);
            return Optional.empty();
        }
    }
    
    /**
     * 提取并验证基本字段
     */
    private static Optional<ModelFields> extractBasicFields(JsonObject jsonObject, Path filePath) {
        // 定义字段验证器
        Function<String, Optional<String>> fieldValidator = fieldName -> 
            validateField(GsonHelper.getAsString(jsonObject, fieldName), fieldName, filePath);
        
        // 提取并验证所有字段
        Optional<String> modelName = fieldValidator.apply("model_name");
        Optional<String> modelID = fieldValidator.apply("model_id");
        Optional<String> modelPath = fieldValidator.apply("model");
        Optional<String> animationPath = fieldValidator.apply("animation");
        Optional<String> texturePath = fieldValidator.apply("texture");
        
        // 如果所有字段都有效，创建ModelFields对象
        if (Stream.of(modelName, modelID, modelPath, animationPath, texturePath)
                .allMatch(Optional::isPresent)) {
            return Optional.of(new ModelFields(
                modelName.get(), modelID.get(), 
                modelPath.get(), animationPath.get(), texturePath.get()
            ));
        }
        
        return Optional.empty();
    }
    
    /**
     * 验证单个字段
     */
    private static Optional<String> validateField(String value, String fieldName, Path filePath) {
        if (value == null || value.trim().isEmpty()) {
            LOGGER.error("{} is null or empty in: {}", fieldName, filePath);
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
    
    /**
     * 加载资源并构建模型数据
     */
    private static Optional<PIAYNModelData> loadResourcesAndBuild(ModelFields fields, Path filePath) {
        Path baseDir = filePath.getParent();
        
        // 构建资源路径
        Path modelPath = baseDir.resolve(fields.modelPath());
        Path animationPath = baseDir.resolve(fields.animationPath());
        Path texturePath = baseDir.resolve(fields.texturePath());
        
        LOGGER.debug("Processing files - Model: {}, Animation: {}, Texture: {}", 
            modelPath, animationPath, texturePath);
        
        // 并行加载资源
        Optional<BakedGeoModel> model = loadModelFromPath(modelPath);
        Optional<BakedAnimations> animation = loadAnimationFromPath(animationPath);
        Optional<ResourceLocation> texture = loadTextureFromPath(fields.modelID(), texturePath);
        
        // 验证所有资源加载成功
        if (Stream.of(model, animation, texture).anyMatch(Optional::isEmpty)) {
            logResourceLoadFailures(model, animation, texture, modelPath, animationPath, texturePath);
            return Optional.empty();
        }
        
        // 构建并返回模型数据
        PIAYNModelData modelData = new PIAYNModelData();
        modelData.setModelName(fields.modelName());
        modelData.setModelID(fields.modelID());
        modelData.setModel(model.get());
        modelData.setAnimation(animation.get());
        modelData.setTexture(texture.get());
        
        LOGGER.info("Successfully loaded model data: {} ({})", fields.modelName(), fields.modelID());
        return Optional.of(modelData);
    }
    
    /**
     * 记录资源加载失败信息
     */
    private static void logResourceLoadFailures(Optional<BakedGeoModel> model, 
                                              Optional<BakedAnimations> animation,
                                              Optional<ResourceLocation> texture,
                                              Path modelPath, Path animationPath, Path texturePath) {
        if (model.isEmpty()) LOGGER.error("Failed to load model from: {}", modelPath);
        if (animation.isEmpty()) LOGGER.error("Failed to load animation from: {}", animationPath);
        if (texture.isEmpty()) LOGGER.error("Failed to load texture from: {}", texturePath);
    }
    
    /**
     * 模型字段记录类
     */
    private record ModelFields(String modelName, String modelID, 
                              String modelPath, String animationPath, String texturePath) {}


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
        return MODEL_DATA.keySet().stream()
            .skip((int) (MODEL_DATA.size() * Math.random()))
            .findFirst()
            .orElse("usagi");
    }
    /**
     * 根据id获取模型数据
     * @param modelId 模型ID
     * @return Optional<PIAYNModelData>
     */
    public static Optional<PIAYNModelData> getModelDataById(String modelId) {
        return Optional.ofNullable(MODEL_DATA.get(modelId));
    }
    /**
     * 根据ID返回模型名字
     * @param modelId 模型ID
     * @return String
     */
    public static String getModelNameById(String modelId) {
        return Optional.ofNullable(MODEL_DATA.get(modelId))
            .map(PIAYNModelData::getModelName)
            .orElse("乌萨奇");
    }
    
}
