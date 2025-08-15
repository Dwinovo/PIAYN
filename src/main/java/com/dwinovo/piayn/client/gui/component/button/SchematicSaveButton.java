package com.dwinovo.piayn.client.gui.component.button;

import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SchematicSaveButton extends Button{
    private static final ResourceLocation BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/component/buttons_icon.png");

    public SchematicSaveButton(int x, int y, OnPress onPress) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    // 按钮尺寸
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 16;
    

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 确定使用哪个材质区域（上半部分或下半部分）
        int textureY = this.isHoveredOrFocused() ? 16 : 0; // 被悬停时使用下半部分（Y=16），否则使用上半部分（Y=0）
        
        // 渲染按钮材质 - 使用x起始为0的区域
        guiGraphics.blit(
            BUTTON_TEXTURE,     // 材质资源
            this.getX(),        // 按钮X位置
            this.getY(),        // 按钮Y位置
            32,                  // 材质源X坐标（主页按钮使用x=0）
            textureY,           // 材质源Y坐标（0或16）
            BUTTON_WIDTH,       // 按钮宽度
            BUTTON_HEIGHT,      // 按钮高度
            128,                // 材质总宽度（entity_container.png的实际宽度）
            128                 // 材质总高度（entity_container.png的实际高度）
        );
    }
    
    @Override
    public void updateWidgetNarration( net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        // 为无障碍功能提供按钮描述
        this.defaultButtonNarrationText(narrationElementOutput);
    }


}
