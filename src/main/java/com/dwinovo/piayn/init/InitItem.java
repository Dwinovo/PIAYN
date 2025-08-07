package com.dwinovo.piayn.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import com.dwinovo.piayn.PIAYN;

public class InitItem {
    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(Registries.ITEM, PIAYN.MOD_ID);

    public static final Supplier<DeferredSpawnEggItem> PET_SPAWN_EGG = ITEM.register("pet_spawn_egg", 
        () -> new DeferredSpawnEggItem(
            InitEntity.PET, // 实体类型的Supplier
            0x5ADBFF, 
            0x3C6997, 
            new Item.Properties()
        ));
}
