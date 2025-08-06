package com.dwinovo.piayn.client.gui.screen.selection.model;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.component.IPetScreenButtons;
import com.dwinovo.piayn.client.gui.component.ModelSwitchButton;
import com.dwinovo.piayn.client.resource.PIAYNLoader;
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
    // 父界面（用于返回）
    private final Screen parentScreen;
    // 主页界面引用
    private final PetContainerScreen homeScreen;

    public PetModelSelectScreen(PetEntity petEntity, Screen parentScreen, PetContainerScreen homeScreen) {
        super(Component.translatable("gui.piayn.entity_model_select"));
        this.petEntity = petEntity;
        this.parentScreen = parentScreen;
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
        this.generateModelSwitchButtons();
    }
    
    /**
     * 生成模型选择按钮
     * 根据可用的模型ID遍历生成按钮，并实现换行布局
     */
    private void generateModelSwitchButtons() {
        // 获取所有可用的模型ID
        Set<String> modelIds = PIAYNLoader.getAllModelIds();
        
        if (modelIds.isEmpty()) {
            PIAYN.LOGGER.warn("No models available for model selection screen");
            return;
        }
        
        // 按钮布局参数
        final int BUTTON_WIDTH = 60;   // 按钮宽度
        final int BUTTON_HEIGHT = 20;  // 按钮高度
        final int BUTTON_SPACING_X = 4; // 按钮水平间距
        final int BUTTON_SPACING_Y = 4; // 按钮垂直间距
        final int BUTTONS_PER_ROW = 2;  // 每行按钮数量（考虑到GUI宽度176，每行2个按钮合适）
        
        // 计算按钮区域的起始位置（在GUI内部，留出边距）
        final int MARGIN_LEFT = 10;     // 左边距
        final int MARGIN_TOP = 40;      // 顶部边距（为主页按钮和模型选择按钮留出空间）
        
        int startX = this.leftPos + MARGIN_LEFT;
        int startY = this.topPos + MARGIN_TOP;
        
        // 遍历生成按钮
        int index = 0;
        for (String modelId : modelIds) {
            // 计算当前按钮的行和列
            int row = index / BUTTONS_PER_ROW;
            int col = index % BUTTONS_PER_ROW;
            
            // 计算按钮位置
            int buttonX = startX + col * (BUTTON_WIDTH + BUTTON_SPACING_X);
            int buttonY = startY + row * (BUTTON_HEIGHT + BUTTON_SPACING_Y);
            
            // 检查是否超出GUI边界
            if (buttonY + BUTTON_HEIGHT > this.topPos + GUI_HEIGHT - 10) {
                // 如果超出边界，停止生成更多按钮
                PIAYN.LOGGER.warn("Model selection area full, some models may not be displayed");
                break;
            }
            
            // 创建模型切换按钮
            ModelSwitchButton modelButton = new ModelSwitchButton(
                buttonX, 
                buttonY, 
                modelId, 
                this.petEntity
            );
            
            // 添加按钮到界面
            this.addRenderableWidget(modelButton);
            
            index++;
        }
        
        PIAYN.LOGGER.info("Generated {} model switch buttons for model selection screen", 
                         Math.min(index, modelIds.size()));
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
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染GUI背景纹理
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        

    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // TODO Auto-generated method stub
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
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
    public void onClose() {
        // 关闭界面时返回父界面
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        // 不暂停游戏
        return false;
    }
}
