package com.dwinovo.piayn.client.gui.screen.schematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchematicSelectScreen extends Screen {
    private final ItemStack targetStack;

    private static final Path MODELS_DIR = Paths.get(System.getProperty("user.dir"), "config", "piayn", "schematics");

    private final List<String> nbtFiles = new ArrayList<>();
    private final List<Button> fileButtons = new ArrayList<>();

    private int left;
    private int top;
    private int panelWidth = 220;
    private int panelHeight = 180;

    public SchematicSelectScreen(ItemStack targetStack) {
        super(Component.literal("选择蓝图 NBT"));
        this.targetStack = targetStack;
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - panelWidth) / 2;
        this.top = (this.height - panelHeight) / 2;

        loadNbtFiles();
        buildButtons();
    }

    private void loadNbtFiles() {
        nbtFiles.clear();
        if (!Files.isDirectory(MODELS_DIR)) {
            try { Files.createDirectories(MODELS_DIR); } catch (IOException ignored) {}
            return;
        }
        try (Stream<Path> s = Files.list(MODELS_DIR)) {
            nbtFiles.addAll(s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".nbt"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            // ignore, keep empty list
        }
    }

    private void buildButtons() {
        this.clearWidgets();
        this.fileButtons.clear();

        int x = left + 10;
        int y = top + 30;
        int btnW = panelWidth - 20;
        int btnH = 20;

        // 简单列表：最多显示 7 条，避免超出界面；其余可按需扩展分页/滚动
        int max = Math.min(7, nbtFiles.size());
        for (int i = 0; i < max; i++) {
            final String file = nbtFiles.get(i);
            Button b = Button.builder(Component.literal(file), btn -> {
                onSelect(file);
            }).pos(x, y + i * (btnH + 4)).size(btnW, btnH).build();
            this.addRenderableWidget(b);
            this.fileButtons.add(b);
        }

        // 关闭按钮
        Button close = Button.builder(Component.literal("关闭"), b -> onClose())
                .pos(left + panelWidth - 70, top + panelHeight - 24)
                .size(60, 16)
                .build();
        this.addRenderableWidget(close);
    }

    private void onSelect(String fileName) {
        // 写入自定义数据：Selected=true, File=fileName
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Selected", true);
        tag.putString("File", fileName);
        targetStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // 提示并关闭界面
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("已选择蓝图: " + fileName), true);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        
        super.render(gfx, mouseX, mouseY, partialTick);
        // 面板背景
        
        gfx.drawString(this.font, this.title, left + 10, top + 10, 0xFFFFFF, false);
        if (nbtFiles.isEmpty()) {
            gfx.drawString(this.font, Component.literal("未找到 .nbt 文件"), left + 10, top + 40, 0xAAAAAA, false);
        }
        
        
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xCC000000);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
