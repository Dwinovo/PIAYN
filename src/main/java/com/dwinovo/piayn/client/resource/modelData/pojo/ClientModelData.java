package com.dwinovo.piayn.client.resource.modelData.pojo;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.object.BakedAnimations;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import net.minecraft.resources.ResourceLocation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientModelData {
    @NonNull
    private String modelName;
    @NonNull
    private String modelID;
    @NonNull
    private BakedGeoModel model;
    @NonNull
    private BakedAnimations animation;
    @NonNull
    private ResourceLocation texture;
}
