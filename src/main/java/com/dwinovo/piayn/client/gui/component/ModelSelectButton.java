package com.dwinovo.piayn.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.screen.container.PetContainerScreen;
import com.dwinovo.piayn.client.gui.screen.selection.model.PetModelSelectScreen;
import com.dwinovo.piayn.entity.PetEntity;

/**
 * 模型选择按钮
 */
public class ModelSelectButton extends Button {
    // 按钮材质路径
    private static final ResourceLocation BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/component/container_button.png");
    
    // 按钮尺寸
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 16;
    
    // 关联的宠物实体、父界面和主页界面
    private final PetEntity petEntity;
    private final Screen currentScreen;
    private final PetContainerScreen homeScreen;
    
    public ModelSelectButton(int x, int y, PetEntity petEntity, Screen currentScreen, PetContainerScreen homeScreen) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(), 
              button -> openModelSelectScreen(petEntity, currentScreen, homeScreen), DEFAULT_NARRATION);
        this.petEntity = petEntity;
        this.currentScreen = currentScreen;
        this.homeScreen = homeScreen;
    }
    
    /**
     * 打开模型选择界面
     * 只有当前界面不是PetModelSelectScreen时才打开
     */
    private static void openModelSelectScreen(PetEntity petEntity, Screen parentScreen, PetContainerScreen homeScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            // 检查当前界面是否已经是模型选择界面
            if (!(parentScreen instanceof PetModelSelectScreen)) {
                PetModelSelectScreen screen = new PetModelSelectScreen(petEntity, parentScreen, homeScreen);
                minecraft.setScreen(screen);
            }
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 确定使用哪个材质区域（上半部分或下半部分）
        int textureY = this.isHoveredOrFocused() ? 16 : 0; // 被点击时使用下半部分（Y=16），否则使用上半部分（Y=0）
        
        // 渲染按钮材质
        guiGraphics.blit(
            BUTTON_TEXTURE,     // 材质资源
            this.getX(),        // 按钮X位置
            this.getY(),        // 按钮Y位置
            16,                  // 材质源X坐标
            textureY,           // 材质源Y坐标（0或16）
            BUTTON_WIDTH,       // 按钮宽度
            BUTTON_HEIGHT,      // 按钮高度
            128,                // 材质总宽度（PNG文件的实际宽度）
            32                 // 材质总高度（PNG文件的实际高度）
        );
    }

    @Override
    public void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        // 为无障碍功能提供按钮描述
        this.defaultButtonNarrationText(narrationElementOutput);
    }
    @Override
    public boolean isHoveredOrFocused() {
        // 如果当前页面是PetContainerScreen，返回true
        return this.currentScreen instanceof PetModelSelectScreen || super.isHoveredOrFocused();
    }
}
