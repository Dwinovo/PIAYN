package com.dwinovo.piayn.init;

import com.dwinovo.piayn.PIAYN;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import com.mojang.serialization.Codec;

public class InitComponent {
    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PIAYN.MOD_ID);

    // 自定义字符串数据组件：用于在物品上保存一个字符串（如示意名称）
    public static final Supplier<DataComponentType<String>> SCHEMATIC_NAME = COMPONENT_TYPES.register(
            "schematic_name",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

}
