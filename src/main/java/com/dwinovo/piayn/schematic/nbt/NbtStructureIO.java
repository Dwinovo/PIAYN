package com.dwinovo.piayn.schematic.nbt;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 基于 Minecraft 原生 StructureTemplate 的 NBT 导入/导出工具
 * - 导出：从选区抓取结构，保存为压缩 .nbt（与 Create 一致的存储路径与读写方式）
 * - 导入：从 .nbt 读取为 StructureTemplate 并按给定设置放置回世界
 */
public final class NbtStructureIO {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Path DEFAULT_DIR = Paths.get(System.getProperty("user.dir"), "config", "piayn", "schematics");

    private NbtStructureIO() {}

    /**
     * 将 Level 中 [first, second] 包围盒选区导出为 StructureTemplate，并保存为压缩 .nbt 文件。
     *
     * @param level           世界
     * @param first           选区一角
     * @param second          选区对角
     * @param dir             目标目录
     * @param fileName        文件名（自动补全 .nbt）
     * @param overwrite       是否允许覆盖
     * @param includeEntities 是否包含实体
     * @return 最终写入的文件路径
     */
    public static Path exportRegionToNbt(Level level,
                                         BlockPos first,
                                         BlockPos second,
                                         String fileName,
                                         boolean overwrite,
                                         boolean includeEntities) throws IOException {
        if (level == null || first == null || second == null)
            throw new IllegalArgumentException("level/first/second must not be null");

        BlockPos min = new BlockPos(Math.min(first.getX(), second.getX()),
                                     Math.min(first.getY(), second.getY()),
                                     Math.min(first.getZ(), second.getZ()));
        BlockPos max = new BlockPos(Math.max(first.getX(), second.getX()),
                                     Math.max(first.getY(), second.getY()),
                                     Math.max(first.getZ(), second.getZ()));
        BlockPos size = new BlockPos(max.getX() - min.getX() + 1,
                                     max.getY() - min.getY() + 1,
                                     max.getZ() - min.getZ() + 1);

        StructureTemplate template = new StructureTemplate();
        // 从世界填充模板（原点使用 min）
        template.fillFromWorld(level, min, size, includeEntities, null);

        // 保存为 NBT
        CompoundTag data = template.save(new CompoundTag());
        Path out = SchematicNbtIO.writeCompressed(DEFAULT_DIR, fileName, overwrite, data);
        LOGGER.info("Exported region {} -> {} (size={}) to {}", min, max, size, out);
        return out;
    }

    /**
     * 从压缩 .nbt 读取 StructureTemplate 并放置到世界。
     *
     * @param level    服务器世界
     * @param dir      目录
     * @param fileName 文件名（.nbt）
     * @param anchor   放置锚点（模板保存时的原点将对齐到该位置）
     * @param settings 放置设置（旋转/镜像/NBT 处理等）
     */
    public static void loadAndPlace(ServerLevel level,
                                    String fileName,
                                    BlockPos anchor,
                                    StructurePlaceSettings settings) throws IOException {
        if (level == null)
            throw new IllegalArgumentException("level is null");
        if (anchor == null)
            throw new IllegalArgumentException("anchor is null");

        CompoundTag nbt = SchematicNbtIO.readCompressed(DEFAULT_DIR, fileName);
        if (nbt == null)
            throw new IOException("Failed to read schematic nbt: " + fileName);

        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK), nbt);

        // 以 anchor 作为模板放置的起点（第二个 origin 也用 anchor，与 Create 的用法一致）
        template.placeInWorld(level, anchor, anchor, settings, level.getRandom(), Block.UPDATE_CLIENTS);
        LOGGER.info("Placed schematic {} at {}", fileName, anchor);
    }
}
