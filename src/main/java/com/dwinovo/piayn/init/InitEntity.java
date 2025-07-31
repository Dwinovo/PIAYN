package com.dwinovo.piayn.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;
@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class InitEntity {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, PIAYN.MOD_ID);

    public static Supplier<EntityType<PetEntity>> PET = ENTITY_TYPES.register("pet", () -> PetEntity.TYPE);

    @SubscribeEvent
    public static void addEntityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(PetEntity.TYPE, PetEntity.createAttributes().build());
    }
}
