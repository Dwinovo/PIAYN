package com.dwinovo.piayn.client.renderer.layer;

import com.dwinovo.piayn.entity.PetEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
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
    
    /**
     * 自定义物品渲染，支持缩放控制
     * 
     * @param poseStack 姿态堆栈，用于变换
     * @param bone 骨骼
     * @param stack 物品堆栈
     * @param animatable 动画实体
     * @param bufferSource 缓冲区源
     * @param partialTick 部分tick
     * @param packedLight 光照值
     * @param packedOverlay 覆盖层光照值
     */
    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, 
                                    PetEntity animatable, MultiBufferSource bufferSource, 
                                    float partialTick, int packedLight, int packedOverlay) {
        
        if (stack.isEmpty()) {
            return;
        }
        
        // 根据骨骼名称设置不同的缩放比例
        if ("RightHandLocator".equals(bone.getName())) {
            // 设置物品缩放 - 可以根据需要调整这些值
            float scale = 0.6f; // 缩放到80%大小
            poseStack.scale(scale, scale, scale);
        }
        // 调用父类的渲染方法
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
    }
    
}
