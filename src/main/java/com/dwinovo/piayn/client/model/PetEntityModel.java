package com.dwinovo.piayn.client.model;



import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.PIAYNLoader;
import com.dwinovo.piayn.entity.PetEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

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
    @Override
    public void setCustomAnimations(PetEntity animatable, long instanceId, AnimationState<PetEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        // 获取模型数据
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        // 获取头部骨骼
        GeoBone headBone = this.getAnimationProcessor().getBone("AllHead");
        // 如果头部骨骼不为空
        if (headBone != null) {
            // 直接访问EntityModelData中的Yaw和Pitch
            float netHeadYaw = modelData.netHeadYaw();  
            float headPitch = modelData.headPitch();   
            // 设置头部骨骼的旋转
            headBone.setRotY(netHeadYaw * ((float) Math.PI / 180F));  
            headBone.setRotX(headPitch * ((float) Math.PI / 180F));   
        }
        
        // 获取耳朵和尾巴的骨骼
        GeoBone leftEarBone = this.getAnimationProcessor().getBone("LeftEar");
        GeoBone rightEarBone = this.getAnimationProcessor().getBone("RightEar");
        GeoBone tailBone = this.getAnimationProcessor().getBone("tail");

        // 获取动画状态中的 ageInTicks (通过 TICK 数据)
        double ageInTicks = animationState.getData(DataTickets.TICK);  // 从DataTickets获取tick数据
        float limbSwingAmount = animationState.getLimbSwingAmount();  // 实体移动的摆动量

        // 呼吸效果的基础频率和幅度
        float breathingSpeed = 0.1F;
        float earSwingAmount = 0.1F;  // 耳朵的前后摆动幅度
        float earTwistAmount = 0.1F;  // 耳朵的左右摆动幅度

        // 耳朵的后摆随着移动速度调整
        float earBackwardSwing = -limbSwingAmount * 1.0F;  // 控制耳朵向后的摆动量

        // 模拟左耳摆动
        if (leftEarBone != null) {
            leftEarBone.setRotY(Mth.cos((float)ageInTicks * breathingSpeed) * earSwingAmount - earBackwardSwing);  // 前后摆动
            leftEarBone.setRotZ(Mth.sin((float)ageInTicks * breathingSpeed) * earTwistAmount);  // 左右摇晃
        }

        // 模拟右耳摆动
        if (rightEarBone != null) {
            rightEarBone.setRotY(-Mth.cos((float)ageInTicks * breathingSpeed) * earSwingAmount + earBackwardSwing);  // 前后摆动
            rightEarBone.setRotZ(-Mth.sin((float)ageInTicks * breathingSpeed) * earTwistAmount);  // 左右摇晃
        }

        // 尾巴的摆动
        if (tailBone != null) {
            tailBone.setRotY(Mth.cos((float)ageInTicks * breathingSpeed) * 0.15F);  // 尾巴的左右摆动
        }
    } 
    
}
