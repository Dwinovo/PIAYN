package com.dwinovo.piayn.schematic.nbt;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 与 Create 对齐的 NBT 压缩读写工具。
 * - 使用 NbtIo.writeCompressed / readCompressed 保存与读取 .nbt 文件
 * - 统一处理 .nbt 后缀
 * - 在非覆盖模式下自动规避重名
 */
public final class SchematicNbtIO {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SchematicNbtIO() {}

    public static final String NBT_EXT = ".nbt";

    /**
     * 确保文件名以 .nbt 结尾。
     */
    public static String ensureNbtExtension(String fileName) {
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException("fileName is null or blank");
        return fileName.endsWith(NBT_EXT) ? fileName : (fileName + NBT_EXT);
    }

    /**
     * 将 NBT（CompoundTag）以 GZIP 压缩格式写入到 dir/fileName（.nbt）。
     * - 当 overwrite=false 且目标已存在时，自动查找可用文件名（追加 _1, _2, ...）。
     * - 返回最终写入的文件路径。
     */
    public static Path writeCompressed(Path dir, String fileName, boolean overwrite, CompoundTag root) throws IOException {
        Objects.requireNonNull(dir, "dir");
        Objects.requireNonNull(root, "root");
        String finalName = ensureNbtExtension(Objects.requireNonNull(fileName, "fileName"));

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path out = dir.resolve(finalName);
        if (!overwrite) {
            out = resolveUniquePath(out);
        }

        NbtIo.writeCompressed(root, out);
        LOGGER.info("Wrote schematic nbt: {}", out);
        return out;
    }

    /**
     * 读取压缩的 .nbt 文件为 CompoundTag。
     */
    public static CompoundTag readCompressed(Path dir, String fileName) throws IOException {
        Objects.requireNonNull(dir, "dir");
        String finalName = ensureNbtExtension(Objects.requireNonNull(fileName, "fileName"));
        Path in = dir.resolve(finalName);
        if (!Files.exists(in))
            throw new IOException("Schematic file does not exist: " + in);

        CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        LOGGER.info("Read schematic nbt: {}", in);
        return tag;
    }

    /**
     * 在非覆盖模式下，为现有路径生成一个不冲突的新路径：
     *   name.nbt -> name_1.nbt -> name_2.nbt -> ...
     */
    public static Path resolveUniquePath(Path desired) {
        if (!Files.exists(desired)) return desired;
        String fileName = desired.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : NBT_EXT;

        int i = 1;
        Path parent = desired.getParent();
        Path candidate;
        do {
            candidate = parent.resolve(base + "_" + i + ext);
            i++;
        } while (Files.exists(candidate));
        return candidate;
    }
}
