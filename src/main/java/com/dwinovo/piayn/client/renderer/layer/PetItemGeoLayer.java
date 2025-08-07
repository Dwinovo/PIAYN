package com.dwinovo.piayn.client.renderer.layer;

import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * 宠物物品渲染层，用于渲染宠物主手持有的物品
 */
public class PetItemGeoLayer extends BlockAndItemGeoLayer<PetEntity> {
    
    public PetItemGeoLayer(GeoRenderer<PetEntity> renderer) {
        super(renderer);
    }

    /**
     * 根据骨骼名称返回对应的物品
     * 当骨骼名称包含手部相关关键词时，返回主手物品
     */
    @Override
    public ItemStack getStackForBone(GeoBone bone, PetEntity animatable) {
        //当Geckolib遍历到RightHandLocator时，返回mainHandItem绑定
        return "RightHandLocator".equals(bone.getName()) ? animatable.getMainHandItem() : ItemStack.EMPTY;
    }

    /**
     * 设置物品的变换类型为第三人称右手持握
     */
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, PetEntity animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
