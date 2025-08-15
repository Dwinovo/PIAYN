package com.dwinovo.piayn.world.schematic.level;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface ISchematicLevelAccessor {
    Set<BlockPos> getAllPositions();
    Map<BlockPos, BlockState> getBlockMap();
    BoundingBox getBounds();
    void setBounds(BoundingBox bounds);
    Iterable<BlockEntity> getBlockEntities();
    Iterable<BlockEntity> getRenderedBlockEntities();
    List<Entity> getEntityList();
}
