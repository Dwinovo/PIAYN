package com.dwinovo.piayn.client.renderer;

import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PetEnityRenderer extends GeoEntityRenderer<PetEntity> {
    public PetEnityRenderer(EntityRendererProvider.Context renderManager, GeoModel<PetEntity> model) {
        super(renderManager, model);
    }
}
