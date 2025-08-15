package com.dwinovo.piayn.client.gui.component.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.modelData.ClientModelDataManager;
import com.dwinovo.piayn.client.resource.modelData.cache.ModelPreviewCache;
import com.dwinovo.piayn.entity.PetEntity;
import com.dwinovo.piayn.packet.ModelSwitchPacket;

/**
 * 模型预览按钮组件
 * <p>用于在GUI界面中展示和切换宠物模型，支持实体预览渲染</p>
 * 
 * @author PIAYN Team
 */
public class ModelPreviewButton extends Button {

    // 按钮材质资源路径
    private static final ResourceLocation BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/component/buttons_icon.png");
    
    // 按钮宽度（像素）
    private static final int BUTTON_WIDTH = 32;
    
    // 按钮高度（像素）
    private static final int BUTTON_HEIGHT = 32;
    
    // 实体渲染缩放比例
    private static final int ENTITY_SCALE = 30;
    
    // 实体Y轴偏移量
    private static final float ENTITY_Y_OFFSET = 0.4F;
    
    // 材质总宽度
    private static final int TEXTURE_WIDTH = 128;
    
    // 材质总高度
    private static final int TEXTURE_HEIGHT = 128;
    
    // 悬停状态材质Y坐标
    private static final int HOVERED_TEXTURE_Y = 64;
    
    // 默认状态材质Y坐标
    private static final int DEFAULT_TEXTURE_Y = 32;
    
    // 模型ID
    private final String modelId;
    
    // 关联的宠物实体
    private final PetEntity petEntity;
    
    // 模型显示名称
    private final String modelName;

    // 当前Screen
    private final Screen currentScreen;

    
    /**
     * 构造模型预览按钮
     * 
     * @param x 按钮X坐标
     * @param y 按钮Y坐标
     * @param modelId 模型ID
     * @param petEntity 关联的宠物实体
     */
    public ModelPreviewButton(int x, int y, String modelId, PetEntity petEntity, Screen currentScreen) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 
              Component.empty(), 
              button -> switchModel(modelId, petEntity), DEFAULT_NARRATION);
        this.modelId = modelId;
        this.petEntity = petEntity;
        this.modelName = ClientModelDataManager.getModelNameById(modelId);
        this.currentScreen = currentScreen;
    }
    
    /**
     * 切换宠物模型
     * <p>发送网络包到服务端请求切换模型</p>
     * 
     * @param targetModelId 目标模型ID
     * @param targetPetEntity 要切换模型的宠物实体
     */
    private static void switchModel(String targetModelId, PetEntity targetPetEntity) {
        // 参数校验
        if (!isValidSwitchRequest(targetModelId, targetPetEntity)) {
            return;
        }
        
        // 避免重复切换
        if (targetModelId.equals(targetPetEntity.getModelID())) {
            return;
        }
        
        sendModelSwitchPacket(targetModelId, targetPetEntity);
        // 切换模型后，关闭当前界面
        Minecraft.getInstance().setScreen(null);
    }
    
    /**
     * 校验模型切换请求的有效性
     * 
     * @param modelId 模型ID
     * @param petEntity 宠物实体
     * @return 是否有效
     */
    private static boolean isValidSwitchRequest(String modelId, PetEntity petEntity) {
        if (petEntity == null || modelId == null) {
            PIAYN.LOGGER.warn("Cannot switch model: petEntity or modelId is null");
            return false;
        }
        
        if (!ClientModelDataManager.getModelDataById(modelId).isPresent()) {
            PIAYN.LOGGER.warn("Model ID not found: {}", modelId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 发送模型切换网络包
     * 
     * @param modelId 模型ID
     * @param petEntity 宠物实体
     */
    private static void sendModelSwitchPacket(String modelId, PetEntity petEntity) {
        try {
            ModelSwitchPacket packet = new ModelSwitchPacket(petEntity.getId(), modelId);
            PacketDistributor.sendToServer(packet);
        } catch (Exception e) {
            PIAYN.LOGGER.error("Failed to send model switch packet for model: {}", modelId, e);
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
        renderButtonBackground(guiGraphics);
        renderEntityPreview(guiGraphics);
    }
    
    /**
     * 渲染按钮背景材质
     * 
     * @param guiGraphics 图形上下文
     */
    private void renderButtonBackground(@NotNull GuiGraphics guiGraphics) {
        int textureY = this.isHoveredOrFocused() ? HOVERED_TEXTURE_Y : DEFAULT_TEXTURE_Y;
        
        guiGraphics.blit(
            BUTTON_TEXTURE,
            this.getX(),
            this.getY(),
            0,
            textureY,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT
        );
    }
    @Override
    public boolean isHoveredOrFocused() {
        return super.isHoveredOrFocused()||this.isCurrentModel();
    }
    
    /**
     * 渲染实体预览
     * 
     * @param guiGraphics 图形上下文
     */
    private void renderEntityPreview(@NotNull GuiGraphics guiGraphics) {
        if (petEntity == null) {
            return;
        }
        
        PetEntity previewEntity = ModelPreviewCache.getInstance().getOrCreateEntity(modelId);
        if (previewEntity == null) {
            return;
        }
        
        renderEntityInButton(guiGraphics, previewEntity);
    }
    
    /**
     * 在按钮区域内渲染实体
     * 
     * @param guiGraphics 图形上下文
     * @param entity 要渲染的实体
     */
    private void renderEntityInButton(@NotNull GuiGraphics guiGraphics, @NotNull PetEntity entity) {
        int buttonCenterX = this.getX() + this.width / 2 + 2;
        int buttonCenterY = this.getY() + this.height / 2 + 2;
        
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                ENTITY_SCALE,
                ENTITY_Y_OFFSET,
                (float) buttonCenterX,
                (float) buttonCenterY,
                entity
            );
        } catch (Exception e) {
            // 静默处理渲染异常，避免影响界面显示
            PIAYN.LOGGER.warn("Entity render failed for model: {}", modelId);
        }
    }
    

    @Override
    public void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        String statusKey = isCurrentModel() ? "selected" : "available";
        Component narrationText = Component.translatable(
            "gui.piayn.model_switch_button.narration", 
            this.modelName, 
            statusKey
        );
        narrationElementOutput.add(
            net.minecraft.client.gui.narration.NarratedElementType.TITLE, 
            narrationText
        );
    }
}
