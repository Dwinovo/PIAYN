package com.dwinovo.piayn.client.gui.screen.model;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.platform.InputConstants;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.component.ModelPreviewButton;
import com.dwinovo.piayn.client.resource.ClientModelDataManager;
import com.dwinovo.piayn.client.gui.screen.IPetScreenButtons;
import com.dwinovo.piayn.client.gui.screen.container.PetContainerScreen;
import com.dwinovo.piayn.entity.PetEntity;
import java.util.Set;

/**
 * 实体模型选择界面
 * 用于选择宠物的显示模型
 */
public class PetModelSelectScreen extends Screen implements IPetScreenButtons {
    // GUI纹理路径
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/screen/entity_model_select_screen.png");
    
    // GUI尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 182;
    
    // GUI位置
    private int leftPos;
    private int topPos;
    
    // 关联的宠物实体
    private final PetEntity petEntity;

    // 主页界面引用
    private final PetContainerScreen homeScreen;

    public PetModelSelectScreen(PetEntity petEntity, PetContainerScreen homeScreen) {
        super(Component.translatable("gui.piayn.entity_model_select"));
        this.petEntity = petEntity;

        this.homeScreen = homeScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        // 计算GUI居中位置
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;
        
        // 使用接口提供的方法初始化宠物按钮（主页按钮和模型选择按钮）
        this.initPetButtons();
        
        // 生成模型选择按钮
        this.addShowModelButton();
    }
    
    /**
     * 生成模型选择按钮
     * 根据可用的模型ID遍历生成按钮，并实现5行5列的网格布局
     */
    private void addShowModelButton() {
        // 获取所有可用的模型ID
        Set<String> modelIds = ClientModelDataManager.getAllModelIds();
        
        if (modelIds.isEmpty()) {
            PIAYN.LOGGER.warn("No models available for model selection screen");
            return;
        }
        
        // 网格布局参数（根据新表格更新为4行5列）
        final int MAX_MODELS_TO_DISPLAY = 20;  // 最多显示20个模型（4行5列）
        final int GRID_ROWS = 4;               // 网格行数（根据表格更新为4行）
        final int GRID_COLUMNS = 5;            // 网格列数（保持5列）
        
        // 按钮布局参数（根据坐标表格的左上角坐标计算）
        // 按钮间的距离（下一个按钮左上角 - 当前按钮左上角）
        final int BUTTON_STEP_X = 34;  // 水平步长（38-4=34）
        final int BUTTON_STEP_Y = 39;  // 垂直步长（64-25=39）
        
        // 按钮区域起始位置（根据新表格的第一个坐标）
        final int BUTTON_START_X = this.leftPos + 4;   // 第一个按钮左上角X坐标
        final int BUTTON_START_Y = this.topPos + 25;   // 第一个按钮左上角Y坐标
        
        // 遍历生成按钮，最多生成20个（4行5列）
        int index = 0;
        for (String modelId : modelIds) {
            if (index >= MAX_MODELS_TO_DISPLAY) {
                PIAYN.LOGGER.warn("Reached maximum display limit of {} models, remaining models will not be shown", MAX_MODELS_TO_DISPLAY);
                break;
            }
            
            // 计算当前按钮在5x5网格中的行和列
            int row = index / GRID_COLUMNS;
            int col = index % GRID_COLUMNS;
            
            // 计算按钮位置（根据表格坐标直接计算）
            int buttonX = BUTTON_START_X + col * BUTTON_STEP_X;
            int buttonY = BUTTON_START_Y + row * BUTTON_STEP_Y;
            
            // 创建模型切换按钮
            ModelPreviewButton modelButton = new ModelPreviewButton(
                buttonX, 
                buttonY, 
                modelId, 
                this.petEntity,
                this
            );
            
            // 添加按钮到界面
            this.addRenderableWidget(modelButton);
            
            index++;
        }
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
        return this.homeScreen;
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
        // 渲染按钮等组件（实体渲染由ShowModelButton负责）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染GUI背景纹理
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        // 在GUI中渲染宠物实体，跟随鼠标转动
                if (this.petEntity != null) {
            // 定义实体渲染区域 (在GUI背景图片左边居中)
            // 背景图片宽176，高182，实体显示在左边80像素宽的区域内
            int entityAreaWidth = 80;         // 实体显示区域宽度
            int entityX1 = this.leftPos - entityAreaWidth;  // 渲染区域左边界
            int entityX2 = this.leftPos;                    // 渲染区域右边界（背景图片左边缘）
            
            // 垂直居中：以背景图片的中心为基准
            int guiCenterY = this.topPos + GUI_HEIGHT / 2;  // 背景图片垂直中心
            int entityAreaHeight = 100;                     // 实体显示区域高度
            int entityY1 = guiCenterY - entityAreaHeight / 2; // 渲染区域上边界
            int entityY2 = guiCenterY + entityAreaHeight / 2; // 渲染区域下边界
            
            int scale = 50;                              // 实体缩放比例
            float yOffset = 0.0F;                        // Y轴偏移（居中不需要偏移）
            
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
    


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            // 按背包键（通常是E键）关闭屏幕，与官方容器界面行为一致
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        // 不暂停游戏
        return false;
    }
}
