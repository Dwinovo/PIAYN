package com.dwinovo.piayn.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.PIAYNLoader;
import com.dwinovo.piayn.entity.PetEntity;
import com.dwinovo.piayn.network.packet.ModelSwitchPacket;

/**
 * 模型切换按钮
 * 用于切换宠物的显示模型
 */
public class ModelSwitchButton extends Button {
    // 按钮材质路径（目前保留为空，后续可添加）
    private static final ResourceLocation BUTTON_TEXTURE = null;
    
    // 按钮尺寸
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    
    // 关联的模型ID和宠物实体
    private final String modelId;
    private final PetEntity petEntity;
    private final String modelName;
    
    public ModelSwitchButton(int x, int y, String modelId, PetEntity petEntity) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 
              Component.literal(PIAYNLoader.getModelNameById(modelId)), 
              button -> switchModel(modelId, petEntity), DEFAULT_NARRATION);
        this.modelId = modelId;
        this.petEntity = petEntity;
        this.modelName = PIAYNLoader.getModelNameById(modelId);
    }
    
    /**
     * 切换宠物模型
     * 发送网络包到服务端请求切换模型
     * @param modelId 目标模型ID
     * @param petEntity 要切换模型的宠物实体
     */
    private static void switchModel(String modelId, PetEntity petEntity) {
        if (petEntity == null || modelId == null) {
            PIAYN.LOGGER.warn("Cannot switch model: petEntity or modelId is null");
            return;
        }
        
        // 验证模型ID是否存在（客户端预检查）
        if (!PIAYNLoader.getModelDataById(modelId).isPresent()) {
            PIAYN.LOGGER.warn("Model ID not found: {}", modelId);
            return;
        }
        
        // 检查是否已经是当前模型
        if (modelId.equals(petEntity.getModelID())) {
            PIAYN.LOGGER.debug("Pet is already using model: {}", modelId);
            return;
        }
        
        try {
            // 创建模型切换网络包
            ModelSwitchPacket packet = new ModelSwitchPacket(
                petEntity.getId(),  // 宠物实体ID
                modelId            // 目标模型ID
            );
            
            // 发送包到服务端
            PacketDistributor.sendToServer(packet);
            
            PIAYN.LOGGER.info("Sent model switch request to server: pet ID {}, target model '{}' ({})", 
                petEntity.getId(), PIAYNLoader.getModelNameById(modelId), modelId);
                
        } catch (Exception e) {
            PIAYN.LOGGER.error("Failed to send model switch packet", e);
        }
    }
    
    /**
     * 获取关联的模型ID
     */
    public String getModelId() {
        return this.modelId;
    }
    
    /**
     * 获取关联的宠物实体
     */
    public PetEntity getPetEntity() {
        return this.petEntity;
    }
    
    /**
     * 获取模型名称
     */
    public String getModelName() {
        return this.modelName;
    }
    
    /**
     * 检查当前按钮是否对应当前宠物的模型
     */
    public boolean isCurrentModel() {
        return this.petEntity != null && this.modelId.equals(this.petEntity.getModelID());
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 如果没有材质，使用默认的按钮渲染
        if (BUTTON_TEXTURE == null) {
            // 根据按钮状态确定颜色
            int backgroundColor;
            int textColor;
            
            if (isCurrentModel()) {
                // 当前选中的模型 - 高亮显示
                backgroundColor = this.isHoveredOrFocused() ? 0xFF4CAF50 : 0xFF388E3C;
                textColor = 0xFFFFFFFF;
            } else if (!this.active) {
                // 禁用状态
                backgroundColor = 0xFF666666;
                textColor = 0xFFA0A0A0;
            } else if (this.isHoveredOrFocused()) {
                // 悬停状态
                backgroundColor = 0xFF2196F3;
                textColor = 0xFFFFFFFF;
            } else {
                // 正常状态
                backgroundColor = 0xFF757575;
                textColor = 0xFFFFFFFF;
            }
            
            // 渲染按钮背景
            guiGraphics.fill(this.getX(), this.getY(), 
                           this.getX() + this.width, this.getY() + this.height, 
                           backgroundColor);
            
            // 渲染按钮边框
            int borderColor = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF000000;
            guiGraphics.hLine(this.getX(), this.getX() + this.width - 1, this.getY(), borderColor);
            guiGraphics.hLine(this.getX(), this.getX() + this.width - 1, this.getY() + this.height - 1, borderColor);
            guiGraphics.vLine(this.getX(), this.getY(), this.getY() + this.height - 1, borderColor);
            guiGraphics.vLine(this.getX() + this.width - 1, this.getY(), this.getY() + this.height - 1, borderColor);
            
            // 渲染按钮文本（居中）
            Component displayText = this.getMessage();
            Font font = Minecraft.getInstance().font;
            int textWidth = font.width(displayText);
            int textX = this.getX() + (this.width - textWidth) / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            
            guiGraphics.drawString(font, displayText, textX, textY, textColor, false);
        } else {
            // 如果有自定义材质，在这里添加材质渲染逻辑
            // TODO: 实现自定义材质渲染
        }
    }

    @Override
    public void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        // 为无障碍功能提供按钮描述
        Component narrationText = Component.translatable("gui.piayn.model_switch_button.narration", 
                                                        this.modelName, 
                                                        isCurrentModel() ? "selected" : "available");
        narrationElementOutput.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, narrationText);
    }
}
