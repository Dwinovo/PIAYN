package com.dwinovo.piayn.client.gui.component;

import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.schematic.nbt.NbtStructureIO;
import com.mojang.logging.LogUtils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.io.IOException;
import java.util.function.Supplier;
import org.slf4j.Logger;

public class SchemSaveButton extends Button{
    private static final ResourceLocation BUTTON_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/component/buttons_icon.png");
    private static final Logger LOGGER = LogUtils.getLogger();

    public SchemSaveButton(int x, int y, Screen currentScreen, Supplier<SaveArgs> dataSupplier) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(),
                button -> {
                    try {
                        SaveArgs args = dataSupplier.get();
                        if (args == null) {
                            LOGGER.warn("SaveArgs supplier returned null");
                            return;
                        }
                        // 执行导出为压缩 NBT
                        NbtStructureIO.exportRegionToNbt(
                                args.level,
                                args.first,
                                args.second,
                                args.fileName,
                                args.overwrite,
                                args.includeEntities
                        );
                        currentScreen.onClose();
                    } catch (IOException e) {
                        LOGGER.error("Failed to save schematic nbt", e);
                    } catch (Exception e) {
                        LOGGER.error("Unexpected error while saving schematic nbt", e);
                    }
                }, DEFAULT_NARRATION);
        this.currentScreen = currentScreen;
    }

    // 按钮尺寸
    private static final int BUTTON_WIDTH = 16;
    private static final int BUTTON_HEIGHT = 16;
    
    // 当前界面引用
    private final Screen currentScreen;

    // 保存参数容器（使用 record 更简洁且不可变）
    public static record SaveArgs(Level level,
                                  BlockPos first,
                                  BlockPos second,
                                  String fileName,
                                  boolean overwrite,
                                  boolean includeEntities) {}


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
    public void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        // 为无障碍功能提供按钮描述
        this.defaultButtonNarrationText(narrationElementOutput);
    }


}
