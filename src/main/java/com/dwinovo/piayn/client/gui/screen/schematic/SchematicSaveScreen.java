package com.dwinovo.piayn.client.gui.screen.schematic;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.component.SchematicSaveButton;
import com.dwinovo.piayn.schematic.nbt.NbtStructureIO;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class SchematicSaveScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private EditBox nameBox;
    private SchematicSaveButton saveButton;


    // 背景贴图：整张纹理为 256x256，但实际可见材质区域为 206x47
    private static final ResourceLocation BG_TEX = ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/gui/schem/schem_save.png");
    private static final int BG_WIDTH = 206;   // 需要绘制的区域宽度
    private static final int BG_HEIGHT = 47;   // 需要绘制的区域高度
    private static final int BG_TEX_W = 256;   // 纹理实际宽度
    private static final int BG_TEX_H = 256;   // 纹理实际高度

    // 背景左上角屏幕坐标
    private int left;
    private int top;
    
    // 数据来源
    private final Level level;
    private final BlockPos pos1;
    private final BlockPos pos2;


    public SchematicSaveScreen(Level level, BlockPos pos1, BlockPos pos2) {
        super(Component.empty());
        this.level = level;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    protected void init() {
        this.left = (this.width - BG_WIDTH) / 2;
        this.top = (this.height - BG_HEIGHT) / 2;

        // 文本框（使用自定义材质子类）
        // 放置在背景相对坐标 (9,16)，大小 141x18
        this.nameBox = new EditBox(this.font, left + 16, top + 26, 140, 18, Component.literal(""));
        this.nameBox.setBordered(false);
        this.nameBox.setMaxLength(128);
        this.nameBox.setValue("");
        this.addRenderableWidget(this.nameBox);

        
        this.saveButton = new SchematicSaveButton(this.left + 169, this.top + 23,
                btn -> {
                    try {
                        String schemticName = this.nameBox.getValue().trim();
                        NbtStructureIO.exportRegionToNbt(this.level, this.pos1, this.pos2, schemticName);
                        this.onClose();
                    } catch (Exception e) {
                        LOGGER.error("error while saving schematic nbt", e);
                    }
                }
        );
        this.addRenderableWidget(this.saveButton);

        this.saveButton.active = false;
        this.nameBox.setResponder(text -> this.saveButton.active = !text.trim().isEmpty());
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 先绘制背景
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, Component.empty(), this.width / 2, top - 12, 0xFFFFFF);

    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(BG_TEX, left, top, 0, 0, BG_WIDTH, BG_HEIGHT, BG_TEX_W, BG_TEX_H);
    }

    
}
