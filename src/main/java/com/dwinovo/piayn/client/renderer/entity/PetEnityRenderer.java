package com.dwinovo.piayn.client.renderer.entity;

import com.dwinovo.piayn.client.renderer.layer.PetItemGeoLayer;
import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PetEnityRenderer extends GeoEntityRenderer<PetEntity> {
    public PetEnityRenderer(EntityRendererProvider.Context renderManager, GeoModel<PetEntity> model) {
        super(renderManager, model);
        
        // 添加宠物物品渲染层，用于渲染主手物品
        this.addRenderLayer(new PetItemGeoLayer(this));
    }
}
