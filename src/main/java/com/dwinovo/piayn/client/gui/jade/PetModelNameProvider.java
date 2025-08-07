package com.dwinovo.piayn.client.gui.jade;

import com.dwinovo.piayn.client.resource.ClientModelDataManager;
import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 宠物实体名称提供者
 * 用于在Jade中显示ModelID而不是默认的实体名称
 */
public enum PetModelNameProvider implements IEntityComponentProvider {
    INSTANCE;
    
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getEntity() instanceof PetEntity petEntity) {
            String modelId = petEntity.getModelID();
            if (modelId != null && !modelId.isEmpty()) {
                try {
                    // 获取模型名称而不是ID
                    String modelName = ClientModelDataManager.getModelNameById(modelId);
                    // 清除现有内容并添加模型名称作为第一行
                    tooltip.clear();
                    tooltip.append(Component.literal(modelName).withStyle(ChatFormatting.WHITE));
                    // 白色加粗
                } catch (Exception e) {
                    // 如果获取模型名称失败，使用ModelID作为后备
                    tooltip.clear();
                    tooltip.append(Component.literal(modelId));
                }
            }
        }
    }
    
    /**
     * 设置优先级
     * MOD名称>-1000
     * 生命值>-10000
     * -15000<entity.name<-7500
     */
    @Override
    public int getDefaultPriority() {
        return -7500;
    }

    @Override
    public ResourceLocation getUid() {
        return PIAYNJadePlugin.PET_MODEL_NAME;
    }
}
