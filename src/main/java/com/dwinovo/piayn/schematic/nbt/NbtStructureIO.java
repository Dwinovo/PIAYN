package com.dwinovo.piayn.schematic.nbt;

import com.dwinovo.piayn.PIAYN;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import cn.hutool.core.io.FileUtil;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/**
 * 参考 Create 的 SchematicExport，将选区导出为压缩 NBT 文件。
 * - 目录：<gameDir>/{modid}/schematics/
 * - 文件名：若未指定后缀，自动添加 .nbt；若不覆盖则自动生成唯一名。
 */
public class NbtStructureIO {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final File SCHEMATIC_DIR = FileUtil.file(System.getProperty("user.dir"), "config", "piayn", "schematics");

    /**
     * 将 first-second 选区导出为 .nbt 文件
     *
     * @param level            世界
     * @param first            角点1
     * @param second           角点2
     * @param schematicName    目标文件名（可不带后缀）
     */
    public static void exportRegionToNbt(Level level,BlockPos first,BlockPos second,String schematicName) throws IOException {
        if (level == null || first == null || second == null || schematicName == null)return;

        //如果文件夹不存在，创建
        FileUtil.mkdir(SCHEMATIC_DIR);
        // 如果文件已经存在，返回
        File target = FileUtil.file(SCHEMATIC_DIR, schematicName);
        if (FileUtil.exist(target)) {
            LOGGER.warn("Schematic already exists: {}", target);
            return;
        }
        // 如果文件名没有后缀，自动添加
        if (!schematicName.endsWith(".nbt")) {
            schematicName += ".nbt";
        }
        
        // 计算包围盒、原点与尺寸（含端点）
        BoundingBox bb = BoundingBox.fromCorners(first, second);
        // 计算包围盒的原点与尺寸
        BlockPos origin = new BlockPos(bb.minX(), bb.minY(), bb.minZ());
        BlockPos bounds = new BlockPos(bb.getXSpan(), bb.getYSpan(), bb.getZSpan());

        // 从世界填充模板
        StructureTemplate structure = new StructureTemplate();
        structure.fillFromWorld(level, origin, bounds, true, Blocks.AIR);
        CompoundTag schematicData = structure.save(new CompoundTag());

        try (OutputStream out = FileUtil.getOutputStream(target)) {
            NbtIo.writeCompressed(schematicData, out);
            LOGGER.debug("Schematic saved");
        } catch (IOException e) {
            LOGGER.error("Failed to save schematic: {}", target, e);
        }
    }

    /**
     * 将 nbt 文件解析成 @StructureTemplate
     * @param level
     * @param schematicName
     * @return
     * @throws IOException
     */
    public static StructureTemplate loadNbtToStructureTemplate(Level level, String schematicName) throws IOException {
        StructureTemplate structureTemplate = new StructureTemplate();
        if (schematicName == null) return structureTemplate;
        FileUtil.mkdir(SCHEMATIC_DIR);

        if (!schematicName.endsWith(".nbt")) {
            schematicName += ".nbt";
        }

        File path = FileUtil.file(SCHEMATIC_DIR, schematicName);
        if (!FileUtil.exist(path)) {
            LOGGER.warn("Schematic not found: {}", path);
            return new StructureTemplate();
        }

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(FileUtil.getInputStream(path))))) {
            CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
            structureTemplate.load(level.holderLookup(Registries.BLOCK), nbt);
        } catch (IOException e) {
            LOGGER.error("Failed to load schematic: {}", path, e);
        }
        return structureTemplate;
    }
}
