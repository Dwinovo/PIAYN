package com.dwinovo.piayn.client.model;



import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.PIAYNLoader;
import com.dwinovo.piayn.entity.PetEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.model.GeoModel;

public class PetEntityModel extends GeoModel<PetEntity>{

    private static final ResourceLocation DEFAULT_MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "geo/entity/wusaqi.geo.json");
    private static final ResourceLocation PIAYN_MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "geo/entity/piayn.geo.json");

    private static final ResourceLocation DEFAULT_ANIMATION_LOCATION = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "animations/entity/wusaqi.animation.json");
    private static final ResourceLocation PIAYN_ANIMATION_LOCATION = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "animations/entity/piayn.animation.json");
    

    @Override
    public ResourceLocation getModelResource(PetEntity animatable) {
        String model_id = animatable.getModelID();
        
        if (PIAYNLoader.getModelDataById(model_id).isPresent()) {
            GeckoLibCache.getBakedModels().put(PIAYN_MODEL_LOCATION, PIAYNLoader.getModelDataById(model_id).get().getModel());
            return PIAYN_MODEL_LOCATION;
        }
        return DEFAULT_MODEL_LOCATION;
    }
    
    
    @Override
    public ResourceLocation getTextureResource(PetEntity animatable) {
        String model_id = animatable.getModelID();
        
        if (PIAYNLoader.getModelDataById(model_id).isPresent()) {
            return PIAYNLoader.getModelDataById(model_id).get().getTexture();
        }
        return ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/entity/wusaqi.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PetEntity animatable) {
        String model_id = animatable.getModelID();
        
        if (PIAYNLoader.getModelDataById(model_id).isPresent()) {
            GeckoLibCache.getBakedAnimations().put(PIAYN_ANIMATION_LOCATION, PIAYNLoader.getModelDataById(model_id).get().getAnimation());
            return PIAYN_ANIMATION_LOCATION;
        }
        return DEFAULT_ANIMATION_LOCATION;
    }
    
}
