package com.dwinovo.piayn.client.gui.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;

/**
 * 宠物GUI界面类
 * 负责渲染宠物的GUI界面
 */
public class PetContainerScreen extends AbstractContainerScreen<PetContainerMenu> {
    // GUI纹理路径
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/container/entity_gui.png");
    
    private final PetEntity petEntity;

    public PetContainerScreen(PetContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.petEntity = menu.getPetEntity();
    }

    @Override
    protected void init() {
        super.init();
        // 初始化GUI组件，如按钮等
        // 可以在这里添加按钮或其他交互元素
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 渲染GUI背景纹理
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        // 在GUI中渲染宠物实体，跟随鼠标转动
        if (this.petEntity != null) {
            // 定义实体渲染区域 (在GUI左侧预留的空间)
            int entityX1 = this.leftPos + 26;   // 渲染区域左上角X
            int entityY1 = this.topPos + 18;   // 渲染区域左上角Y  
            int entityX2 = this.leftPos + 78;  // 渲染区域右下角X (70像素宽)
            int entityY2 = this.topPos + 70;   // 渲染区域右下角Y (70像素高)
            int scale = 30;                    // 实体缩放比例
            float yOffset = 0.3F;              // Y轴偏移
            
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                entityX1, entityY1,    // 渲染区域左上角
                entityX2, entityY2,    // 渲染区域右下角  
                scale,                 // 缩放比例
                yOffset,               // Y轴偏移
                (float) mouseX,        // 鼠标X坐标
                (float) mouseY,        // 鼠标Y坐标
                this.petEntity         // 要渲染的宠物实体
            );
        }
    }

    

}
