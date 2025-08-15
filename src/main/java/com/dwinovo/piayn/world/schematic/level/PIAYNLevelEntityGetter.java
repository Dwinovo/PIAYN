package com.dwinovo.piayn.world.schematic.level;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

public class PIAYNLevelEntityGetter<T extends EntityAccess> implements LevelEntityGetter<T> {

    @Override
    public T get(int id) {
        return null;
    }

    @Override
    public T get(UUID uuid) {
        return null;
    }

    @Override
    public Iterable<T> getAll() {
        return Collections.emptyList();
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> test, AbortableIterationConsumer<U> consumer) {
    }

    @Override
    public void get(AABB aabb, Consumer<T> consumer) {
    }

    @Override
    public <U extends T> void get(EntityTypeTest<T, U> test, AABB aabb, AbortableIterationConsumer<U> consumer) {
    }
}
