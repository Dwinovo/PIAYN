package com.dwinovo.piayn.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.entity.PetEntity;

/**
 * 实体模型选择界面
 * 用于选择宠物的显示模型
 */
public class EntityModelSelectScreen extends Screen {
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

    public EntityModelSelectScreen(PetEntity petEntity, Screen parentScreen) {
        super(Component.translatable("gui.piayn.entity_model_select"));
        this.petEntity = petEntity;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        // 计算GUI居中位置
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染GUI背景纹理
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        

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
