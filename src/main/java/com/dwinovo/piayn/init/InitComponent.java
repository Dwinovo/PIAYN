package com.dwinovo.piayn.init;

import com.dwinovo.piayn.PIAYN;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import com.mojang.serialization.Codec;

public class InitComponent {
    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PIAYN.MOD_ID);

    // Schematic Name
    public static final Supplier<DataComponentType<String>> SCHEMATIC_NAME = COMPONENT_TYPES.register(
            "schematic_name",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build()
    );

    // Schematic Anchor
    public static final Supplier<DataComponentType<BlockPos>> SCHEMATIC_ANCHOR = COMPONENT_TYPES.register(
            "schematic_anchor",
            () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build()
    );

}
