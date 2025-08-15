package com.dwinovo.piayn.client.gui.jade;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * PIAYN模组的Jade插件
 * 用于自定义实体显示信息
 */
@WailaPlugin
public class PIAYNJadePlugin implements IWailaPlugin {
    
    public static final ResourceLocation PET_MODEL_NAME = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "pet_model_name");
    
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 注册宠物实体的名称提供者
        registration.registerEntityComponent(PetModelNameProvider.INSTANCE, PetEntity.class);
    }
}
