package com.dwinovo.piayn.server.resource;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 模型目录初始化工具类
 * 负责检查和初始化模型目录，自动解压默认模型数据
 */
public class ServerModelDataInitializer {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path DATA_PATH = Paths.get(System.getProperty("user.dir"), "config", "piayn", "models");
    private static final String MODEL_DATA_PATH = "/assets/piayn/model_data.zip";
    /**
     * 初始化模型目录，如果目录为空或不存在，则从内置资源解压默认模型数据
     */
    public static void initializeModelDirectory() {
        try {
            File modelDir = DATA_PATH.toFile();
            
            // 检查目录是否需要初始化
            if (shouldInitializeDirectory(modelDir)) {
                LOGGER.debug("model directory is empty or does not exist, starting to initialize default model data...");
                // 创建目录
                if (!FileUtil.exist(modelDir)) {
                    FileUtil.mkdir(modelDir);
                    LOGGER.debug("created model directory: {}", modelDir.getAbsolutePath());
                }
                // 解压默认模型数据
                extractDefaultModelData(modelDir);
            } 
        } catch (Exception e) {
            LOGGER.error("failed to initialize model directory", e);
        }
    }
    
    /**
     * 检查目录是否需要初始化
     * @param modelDir 模型目录
     * @return 如果目录不存在或为空则返回true
     */
    private static boolean shouldInitializeDirectory(File modelDir) {
        if (!FileUtil.exist(modelDir)) {
            return true;
        }
        
        // 检查目录是否为空（没有子目录）
        File[] subDirs = modelDir.listFiles(File::isDirectory);
        return subDirs == null || subDirs.length == 0;
    }
    
    /**
     * 从内置资源解压默认模型数据
     * @param targetDir 目标目录
     */
    private static void extractDefaultModelData(File targetDir) {
        
        
        try (InputStream zipStream = ServerModelDataInitializer.class.getResourceAsStream(MODEL_DATA_PATH)) {
            if (zipStream == null) {
                LOGGER.warn("failed to find default model data file: {}", MODEL_DATA_PATH);
                return;
            }
            // 创建临时文件
            File tempZipFile = File.createTempFile("piayn_model_data", ".zip");
            tempZipFile.deleteOnExit();
            // 将输入流写入临时文件
            FileUtil.writeFromStream(zipStream, tempZipFile);
            // 使用Hutool解压到目标目录
            ZipUtil.unzip(tempZipFile, targetDir);
            // 清理临时文件
            FileUtil.del(tempZipFile);
        } catch (IOException e) {
            LOGGER.error("failed to extract default model data", e);
        } catch (Exception e) {
            LOGGER.error("failed to extract default model data", e);
        }
    }
}
