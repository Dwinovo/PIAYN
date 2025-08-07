package com.dwinovo.piayn.server.resource;

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import com.dwinovo.piayn.server.resource.pojo.ServerModelData;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import net.minecraft.util.GsonHelper;
import software.bernie.geckolib.loading.json.typeadapter.KeyFramesAdapter;

public class ServerModelDataLoader {
     public static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ServerModelData> MODEL_DATA = new ConcurrentHashMap<>();
    private static final Path DATA_PATH = Paths.get(System.getProperty("user.dir"), "config", "piayn", "models");

    /**
     * 获取所有已加载的模型数据
     * @return 模型数据映射表
     */
    public static Map<String, ServerModelData> getModelDataMap() {
        return new ConcurrentHashMap<>(MODEL_DATA);
    }
    /**
     * 清空服务器模型数据
     */
    public static void clearModelData() {
        MODEL_DATA.clear();
    }
    
    /**
     * 根据模型ID获取模型数据
     * @param modelID 模型ID
     * @return 模型数据，如果不存在则返回null
     */
    public static ServerModelData getModelData(String modelID) {
        return MODEL_DATA.get(modelID);
    }
    
    /**
     * 检查模型是否存在
     * @param modelID 模型ID
     * @return 是否存在
     */
    public static boolean hasModel(String modelID) {
        return MODEL_DATA.containsKey(modelID);
    }
    
    /**
     * 服务端模型数据加载入口
     */
    public static void ModelDataLoadEnterPoint() {
        
        Optional.of(DATA_PATH.toFile())
            .filter(FileUtil::exist)
            .map(ServerModelDataLoader::getSubDirectories)
            .orElseGet(() -> {
                LOGGER.warn("Resource directory does not exist: {}", DATA_PATH);
                return Stream.empty();
            })
            .forEach(ServerModelDataLoader::processDirectory);
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
                .map(ServerModelDataLoader::parseMainJson)
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
     * @return ServerModelData对象，如果加载失败或参数不完整则返回空
     */
    public static Optional<ServerModelData> parseMainJson(Path filePath) {
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

    private record ModelFields(String modelName, String modelID, 
    String modelPath, String animationPath, String texturePath) {}

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
    private static Optional<ServerModelData> loadResourcesAndBuild(ModelFields fields, Path filePath) {
        Path baseDir = filePath.getParent();
        
        // 构建资源路径
        Path modelPath = baseDir.resolve(fields.modelPath());
        Path animationPath = baseDir.resolve(fields.animationPath());
        Path texturePath = baseDir.resolve(fields.texturePath());
        
        LOGGER.debug("Processing files - Model: {}, Animation: {}, Texture: {}", 
            modelPath, animationPath, texturePath);
        
        // 并行加载资源
        Optional<byte[]> model = loadModelFromPath(modelPath);
        Optional<byte[]> animation = loadAnimationFromPath(animationPath);
        Optional<byte[]> texture = loadTextureFromPath(texturePath);
        
        // 验证所有资源加载成功
        if (Stream.of(model, animation, texture).anyMatch(Optional::isEmpty)) {
            logResourceLoadFailures(model, animation, texture, modelPath, animationPath, texturePath);
            return Optional.empty();
        }
        
        // 构建并返回模型数据
        ServerModelData modelData = new ServerModelData();
        modelData.setModelName(fields.modelName());
        modelData.setModelID(fields.modelID());
        modelData.setModel(model.get());
        modelData.setAnimation(animation.get());
        modelData.setTexture(texture.get());
        
        LOGGER.debug("Successfully loaded model data: {} ({})", fields.modelName(), fields.modelID());
        return Optional.of(modelData);
    }

    /**
     * 记录资源加载失败信息
     */
    private static void logResourceLoadFailures(Optional<byte[]> model, 
                                              Optional<byte[]> animation,
                                              Optional<byte[]> texture,
                                              Path modelPath, Path animationPath, Path texturePath) {
        if (model.isEmpty()) LOGGER.error("Failed to load model from: {}", modelPath);
        if (animation.isEmpty()) LOGGER.error("Failed to load animation from: {}", animationPath);
        if (texture.isEmpty()) LOGGER.error("Failed to load texture from: {}", texturePath);
    }

     /**
     * 从路径加载模型文件并转换为byte[]
     * @param filePath 模型文件的绝对路径
     * @return Optional<byte[]>
     */
    public static Optional<byte[]> loadModelFromPath(Path filePath) {
        try {
            // 读取文件内容
            byte[] model = FileUtil.readBytes(filePath.toFile());
            
            LOGGER.debug("Successfully loaded model from path: {}", filePath);
            return Optional.of(model);
        } catch (Exception e) {
            LOGGER.error("Failed to load model from path: {}", filePath, e);
            return Optional.empty();
        }
    }
    /**
     * 从路径加载动画文件并转换为byte[]
     * @param filePath 动画文件的绝对路径
     * @return Optional<byte[]>
     */
    public static Optional<byte[]> loadAnimationFromPath(Path filePath) {
        try {
            byte[] animation = FileUtil.readBytes(filePath.toFile());
            LOGGER.debug("Successfully loaded animation from path: {}", filePath);
            return Optional.of(animation);
        } catch (Exception e) {
            LOGGER.error("Failed to load animation from path: {}", filePath, e);
            return Optional.empty();
        }
    }
    /**
     * 从任意路径加载纹理文件
     * @param filePath 纹理文件的绝对路径
     * @return Optional<byte[]>
     */
    public static Optional<byte[]> loadTextureFromPath(Path filePath) {
        try {
            byte[] texture = FileUtil.readBytes(filePath.toFile());
            LOGGER.debug("Successfully loaded texture from path: {}", filePath);
            return Optional.of(texture);
        } catch (Exception e) {
            LOGGER.error("Failed to load texture from path: {}", filePath, e);
            return Optional.empty();
        }
    }

}
