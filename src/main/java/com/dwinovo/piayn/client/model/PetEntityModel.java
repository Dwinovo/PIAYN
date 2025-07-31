package com.dwinovo.piayn.client.model;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PetEntityModel extends GeoModel<PetEntity>{

    @Override
    public ResourceLocation getModelResource(PetEntity animatable) {
       
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "geo/entity/wusaqi.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PetEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/entity/wusaqi.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PetEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "animations/entity/wusaqi.animation.json");
    }
    
}
