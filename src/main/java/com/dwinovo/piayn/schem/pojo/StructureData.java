package com.dwinovo.piayn.schem.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructureData {
    @Nonnull ServerLevel serverLevel;
    @Nonnull BlockPos startPos;
    @Nonnull BlockPos endPos;
    @Nullable String schemName;
    @Nullable String authorName;


    /**
     * 返回结构的宽度
     */
    public int getWidth() {
        final int minX = Math.min(startPos.getX(), endPos.getX());
        final int maxX = Math.max(startPos.getX(), endPos.getX());
        final int width = maxX - minX + 1;
        return width;
    }

    /**
     * 返回结构的高度
     */
    public int getHeight() {
        final int maxY = Math.max(startPos.getY(), endPos.getY());
        final int minY = Math.min(startPos.getY(), endPos.getY());
        final int height = maxY - minY + 1;
        return height;
    }

    /**
     * 返回结构的长度
     */
    public int getLength() {
        final int minZ = Math.min(startPos.getZ(), endPos.getZ());
        final int maxZ = Math.max(startPos.getZ(), endPos.getZ());
        final int length = maxZ - minZ + 1;
        return length;
    }
    /**
     * 返回结构的originPos
     */
    public BlockPos getOriginPos() {
        final int minX = Math.min(startPos.getX(), endPos.getX());
        final int minY = Math.min(startPos.getY(), endPos.getY());
        final int minZ = Math.min(startPos.getZ(), endPos.getZ());
        return new BlockPos(minX, minY, minZ);
    }
    
}
