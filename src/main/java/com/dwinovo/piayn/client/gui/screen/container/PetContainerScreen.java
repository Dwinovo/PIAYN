package com.dwinovo.piayn.client.gui.screen.container;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.component.IPetScreenButtons;
import com.dwinovo.piayn.entity.PetEntity;

/**
 * 宠物GUI界面类
 * 负责渲染宠物的GUI界面
 */
public class PetContainerScreen extends AbstractContainerScreen<PetContainerMenu> implements IPetScreenButtons {
    // GUI纹理路径
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/screen/entity_container.png");
    
    private final PetEntity petEntity;

    public PetContainerScreen(PetContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.petEntity = menu.getPetEntity();
        this.imageWidth = 176;
        this.imageHeight = 182;
        
        // 设置标签位置，避免默认位置显示不正确
        this.titleLabelX = 8;  // 容器标题X位置
        this.titleLabelY = 22;  // 容器标题Y位置
        this.inventoryLabelX = 8;  // 玩家物品栏标签X位置
        this.inventoryLabelY = this.imageHeight - 94;  // 玩家物品栏标签Y位置
    }

    @Override
    protected void init() {
        super.init();
        // 初始化GUI组件，如按钮等
        // 可以在这里添加按钮或其他交互元素
        
        // 使用接口提供的方法初始化宠物按钮
        this.initPetButtons();
    }
    
    // 实现PetScreenButtons接口的方法
    @Override
    public PetEntity getPetEntity() {
        return this.petEntity;
    }
    
    @Override
    public int getLeftPos() {
        return this.leftPos;
    }
    
    @Override
    public int getTopPos() {
        return this.topPos;
    }
    
    @Override
    public Screen getScreen() {
        return this;
    }
    
    @Override
    public PetContainerScreen getHomeScreen() {
        return this; // PetContainerScreen本身就是主页界面
    }
    
    @Override
    public void addRenderableWidget(net.minecraft.client.gui.components.Renderable renderable) {
        if (renderable instanceof net.minecraft.client.gui.components.events.GuiEventListener && 
            renderable instanceof net.minecraft.client.gui.narration.NarratableEntry) {
            super.addRenderableWidget((net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.narration.NarratableEntry & net.minecraft.client.gui.components.Renderable) renderable);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 先渲染背景
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 然后渲染工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 渲染容器标题（宠物名称或自定义标题）
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // 渲染玩家物品栏标签
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 渲染GUI背景纹理
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        // 在GUI中渲染宠物实体，跟随鼠标转动
        if (this.petEntity != null) {
            // 定义实体渲染区域 (在GUI左侧预留的空间)
            int entityX1 = this.leftPos + 26;   // 渲染区域左上角X
            int entityY1 = this.topPos + 38;   // 渲染区域左上角Y  
            int entityX2 = this.leftPos + 78;  // 渲染区域右下角X (70像素宽)
            int entityY2 = this.topPos + 86;   // 渲染区域右下角Y (70像素高)
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
