package com.dwinovo.piayn.client.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import com.dwinovo.piayn.entity.PetEntity;
import com.dwinovo.piayn.client.gui.component.button.HomeButton;
import com.dwinovo.piayn.client.gui.component.button.ModelSelectButton;
import com.dwinovo.piayn.client.gui.screen.container.PetContainerScreen;

/**
 * 宠物界面按钮管理接口
 * 提供通用的按钮功能（主页返回按钮和模型选择按钮）
 */
public interface IScreenButtonProvider {
    
    /**
     * 获取关联的宠物实体
     */
    PetEntity getPetEntity();
    
    /**
     * 获取GUI左上角X坐标
     */
    int getLeftPos();
    
    /**
     * 获取GUI左上角Y坐标
     */
    int getTopPos();
    
    /**
     * 获取当前界面实例
     */
    Screen getScreen();
    
    /**
     * 获取主页界面实例（PetContainerScreen）
     * 用于HomeButton导航回主页
     */
    PetContainerScreen getHomeScreen();
    
    /**
     * 添加可渲染组件到界面
     */
    void addRenderableWidget(net.minecraft.client.gui.components.Renderable renderable);
    
    /**
     * 初始化宠物相关的通用按钮
     * 实现类应在init()方法中调用此方法
     */
    default void initPetButtons() {
        
        // 创建主页返回按钮
        HomeButton homeButton = new HomeButton(
            getLeftPos(),       // 按钮X位置（GUI左上角）
            getTopPos(),        // 按钮Y位置（GUI顶部）
            getPetEntity(),     // 宠物实体
            getHomeScreen(),    // 主页界面引用（PetContainerScreen）
            getScreen()         // 当前界面引用
        );
        
        // 创建模型选择按钮
        ModelSelectButton modelSelectButton = new ModelSelectButton(
            getLeftPos() + 16,  // 按钮X位置（主页按钮右侧）
            getTopPos(),        // 按钮Y位置（GUI顶部）
            getPetEntity(),     // 宠物实体
            getHomeScreen(),    // 主页界面引用
            getScreen()         // 当前界面作为父界面
        );
        
        // 将按钮添加到GUI中
        addRenderableWidget(homeButton);
        addRenderableWidget(modelSelectButton);
    }
}
