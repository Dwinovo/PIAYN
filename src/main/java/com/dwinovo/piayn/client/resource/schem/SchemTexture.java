package com.dwinovo.piayn.client.resource.schem;

import net.createmod.catnip.render.BindableTexture;
import net.minecraft.resources.ResourceLocation;

public class SchemTexture implements BindableTexture{

    private ResourceLocation location;

    public SchemTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

}
