package com.dwinovo.piayn.schem;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import com.dwinovo.piayn.schem.pojo.StructureData;
import com.dwinovo.piayn.schem.util.EntityUtil;
import com.dwinovo.piayn.schem.util.BlockUtil;
import com.dwinovo.piayn.schem.util.BlockContainerUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtAccounter;


public class SchemSerializer {

    private static final Logger LOGGER = LogUtils.getLogger();
    
    // === 格式常量 ===
    public static final int SCHEMATIC_VERSION = 3;
    public static final String SCHEMATIC_KEY = "Schematic";
    
    // === 字段名常量 ===
    public static final String VERSION_KEY = "Version";
    public static final String DATA_VERSION_KEY = "DataVersion";
    public static final String WIDTH_KEY = "Width";
    public static final String HEIGHT_KEY = "Height";
    public static final String LENGTH_KEY = "Length";
    public static final String OFFSET_KEY = "Offset";
    public static final String METADATA_KEY = "Metadata";
    public static final String BLOCKS_KEY = "Blocks";
    public static final String ENTITIES_KEY = "Entities";
    
    // === Block Container 字段 ===
    public static final String PALETTE_KEY = "Palette";
    public static final String DATA_KEY = "Data";
    public static final String BLOCK_ENTITIES_KEY = "BlockEntities";
    
    // === Metadata 字段 ===
    public static final String NAME_KEY = "Name";
    public static final String AUTHOR_KEY = "Author";
    public static final String DATE_KEY = "Date";
    
    // === 默认保存目录 ===
    public static final Path DEFAULT_DIR = Paths.get(System.getProperty("user.dir"), "config", "piayn", "schematics");


    /**
     * 把地图结构序列化成CompoundTag
     */
    public static CompoundTag serialize(StructureData structureInfo) {
        final String schematicName = structureInfo.getSchemName();
        final String authorName = structureInfo.getAuthorName();
        final ServerLevel serverLevel = structureInfo.getServerLevel();
        final int width = structureInfo.getWidth();
        final int height = structureInfo.getHeight();
        final int length = structureInfo.getLength();
        final BlockPos originPos = structureInfo.getOriginPos();
        
        // 构建schematic NBT结构
        CompoundTag schematicTag = new CompoundTag();
        
        // 基本信息
        schematicTag.putInt(VERSION_KEY, SCHEMATIC_VERSION);
        schematicTag.putInt(DATA_VERSION_KEY, 3953); // Minecraft 1.21.1 data version
        schematicTag.putShort(WIDTH_KEY, (short) width);
        schematicTag.putShort(HEIGHT_KEY, (short) height);
        schematicTag.putShort(LENGTH_KEY, (short) length);
        schematicTag.put(OFFSET_KEY, new IntArrayTag(new int[]{0, 0, 0}));
        
        // 元数据
        if (schematicName != null || authorName != null) {
            CompoundTag metadataTag = new CompoundTag();
            if (schematicName != null) {
                metadataTag.putString(NAME_KEY, schematicName);
            }
            if (authorName != null) {
                metadataTag.putString(AUTHOR_KEY, authorName);
            }
            metadataTag.putLong(DATE_KEY, System.currentTimeMillis());
            schematicTag.put(METADATA_KEY, metadataTag);
        }
        
        // 构建方块容器 - 暂时使用 SchemNbt 常量
        CompoundTag blocksContainer = BlockContainerUtil.buildBlockContainer(serverLevel, originPos, width, height, length);
        schematicTag.put(BLOCKS_KEY, blocksContainer);
        
        // 构建实体列表
        ListTag entitiesList = EntityUtil.buildEntitiesList(serverLevel, originPos, width, height, length);
        if (!entitiesList.isEmpty()) {
            schematicTag.put(ENTITIES_KEY, entitiesList);
        }
        // 按照Sponge规范构建正确的NBT结构
        CompoundTag rootContent = new CompoundTag();
        rootContent.put(SCHEMATIC_KEY, schematicTag);
        
        return rootContent;
    }

    /**
     * 把SchemCompoundTag在世界中反序列化
     */
    public static void deserialize(CompoundTag schemCompoundTag, ServerLevel serverLevel, BlockPos targetPos) {
        try {
            // 获取 Schematic 根标签
            CompoundTag schematicTag = schemCompoundTag.getCompound("Schematic");
            
            // 粘贴方块
            BlockUtil.pasteBlocks(serverLevel, schematicTag, targetPos);
            
            // 粘贴实体
            EntityUtil.pasteEntities(serverLevel, schematicTag, targetPos);
            
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize schematic", e);
        }
    }


    
    /**
     * 把一个CompoundTag保存成.schem文件
     * 使用GZip压缩的NBT格式，符合Sponge Schematic V3规范
     */
    public static void writeSchem(CompoundTag schemCompoundTag, String filename) {
        try {
            // 确保默认目录存在
            if (!Files.exists(DEFAULT_DIR)) {
                Files.createDirectories(DEFAULT_DIR);
            }
            
            Path filePath = DEFAULT_DIR.resolve(filename);
            
            // 使用GZip压缩写入NBT文件
            NbtIo.writeCompressed(schemCompoundTag, filePath);
            LOGGER.info("Successfully saved schematic to: {}", filePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to write schematic file: {}", filename, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while writing schematic: {}", filename, e);
        }
    }
    
    /**
     * 把一个.schem文件读取成CompoundTag
     * 支持GZip压缩的NBT格式，符合Sponge Schematic V3规范
     */
    public static CompoundTag readSchem(String filename) {
        try {
            Path filePath = DEFAULT_DIR.resolve(filename);
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                LOGGER.error("Schematic file does not exist: {}", filePath);
                return null;
            }
            
            // 读取压缩的NBT文件
            CompoundTag rootTag = NbtIo.readCompressed(filePath, NbtAccounter.unlimitedHeap());
            
            LOGGER.info("Successfully loaded schematic from: {}", filePath);
            return rootTag;
            
        } catch (IOException e) {
            LOGGER.error("Failed to read schematic file: {}", filename, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Unexpected error while reading schematic: {}", filename, e);
            return null;
        }
    }

}
