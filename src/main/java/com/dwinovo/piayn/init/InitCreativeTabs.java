package com.dwinovo.piayn.init;

import com.dwinovo.piayn.PIAYN;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class InitCreativeTabs {
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PIAYN.MOD_ID);

    public static final Supplier<CreativeModeTab> PIAYN_TAB = CREATIVE_MODE_TABS.register("piayn_tab", 
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.piayn.piayn_tab"))
            .icon(() -> new ItemStack(InitItem.PET_SPAWN_EGG.get()))
            .displayItems((parameters, output) -> {
                // 添加模组的所有物品到创造模式标签页
                output.accept(InitItem.PET_SPAWN_EGG.get());
                output.accept(InitItem.BLUEPRINT_PEN.get());
                // 在这里可以继续添加其他物品
                // output.accept(其他物品);
            })
            .build()
    );
}
