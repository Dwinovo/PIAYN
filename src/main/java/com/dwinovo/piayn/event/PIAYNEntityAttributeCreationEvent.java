package com.dwinovo.piayn.event;

import com.dwinovo.piayn.entity.PetEntity;
import com.dwinovo.piayn.PIAYN;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PIAYNEntityAttributeCreationEvent {
    @SubscribeEvent
    public static void addEntityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(PetEntity.TYPE, PetEntity.createAttributes().build());
    }
}
