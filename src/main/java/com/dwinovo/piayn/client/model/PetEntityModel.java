package com.dwinovo.piayn.client.model;



import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.ResourceManager;
import com.dwinovo.piayn.entity.PetEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.model.GeoModel;

public class PetEntityModel extends GeoModel<PetEntity>{

    @Override
    public ResourceLocation getModelResource(PetEntity animatable) {
        GeckoLibCache.getBakedModels().put(ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "geo/entity/wusaqi.geo.json"), ResourceManager.getCustomModel("xiaoba"));
        // 使用自定义模型系统
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "geo/entity/wusaqi.geo.json");
    }
    
    
    @Override
    public ResourceLocation getTextureResource(PetEntity animatable) {
        return ResourceManager.getCustomTexture("xiaoba");
    }

    @Override
    public ResourceLocation getAnimationResource(PetEntity animatable) {
        GeckoLibCache.getBakedAnimations().put(ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "animations/entity/wusaqi.animation.json"), ResourceManager.getCustomAnimation("xiaoba"));
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "animations/entity/wusaqi.animation.json");
    }
    
}
