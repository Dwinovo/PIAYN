package com.dwinovo.piayn.client.gui.screen.schematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.dwinovo.piayn.client.resource.schematic.ClientSchematicManager;
import com.dwinovo.piayn.init.InitComponent;
import com.dwinovo.piayn.packet.SchematicSelectPacket;
import com.dwinovo.piayn.world.schematic.io.NbtStructureIO;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 蓝图选择界面（客户端）。
 *
 * 职责与数据流：
 * - 从本地目录 {@code config/piayn/schematics} 列出 .nbt 文件。
 * - 玩家点击某个文件名后，加载为 {@link StructureTemplate}，
 *   使用 {@link ClientSchematicManager} 创建 {@link SchematicLevel}（局部坐标，原点放置），
 *   并将被选文件名写入玩家物品的 {@code InitComponent.SCHEMATIC_NAME} 数据组件。
 * - 不直接参与渲染，渲染由 {@code SchematicPaperClientEvent} 在 {@code RenderLevelStageEvent} 中完成。
 *
 * 设计取舍：
 * - 界面逻辑尽量简洁，仅展示文件列表和选择反馈；
 * - 只显示最多 7 条，避免做滚动/分页逻辑（可未来扩展）；
 * - 不缓存模板与关卡，由 {@link ClientSchematicManager} 负责当前活跃关卡的生命周期。
 */
public class SchematicSelectScreen extends Screen {
    private final ItemStack targetStack;
    private final InteractionHand hand;

    /** 本地蓝图目录：config/piayn/schematics（以游戏运行目录为根）。 */
    private static final Path MODELS_DIR = Paths.get(System.getProperty("user.dir"), "config", "piayn", "schematics");

    private final List<String> nbtFiles = new ArrayList<>();
    private final List<Button> fileButtons = new ArrayList<>();

    private int left;
    private int top;
    private int panelWidth = 220;
    private int panelHeight = 180;

    /**
     * @param targetStack 客户端侧用于本地预览（非必要写入）。
     * @param hand        打开发送时所用的手，用于服务端定位物品写入组件。
     */
    public SchematicSelectScreen(ItemStack targetStack, InteractionHand hand) {
        super(Component.literal("选择蓝图 NBT"));
        this.targetStack = targetStack;
        this.hand = hand;
    }

    @Override
    /** 初始化布局与列表（进入界面或窗口尺寸变化时调用）。 */
    protected void init() {
        super.init();
        this.left = (this.width - panelWidth) / 2;
        this.top = (this.height - panelHeight) / 2;

        loadNbtFiles();
        buildButtons();
    }

    /**
     * 扫描本地目录，收集 .nbt 文件名（不含路径）。
     * - 若目录不存在则创建空目录并返回空列表。
     * - 失败时静默处理，界面提示“未找到 .nbt 文件”。
     */
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

    /**
     * 构建按钮列表（最多显示 7 条，避免滚动条复杂度）。
     * 可扩展：
     * - 分页/滚动视图；
     * - 文件预览缩略图；
     * - 搜索过滤。
     */
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

    /**
     * 选择指定文件：
     * 1) 使用 {@link NbtStructureIO#loadNbtToStructureTemplate(Level, String)} 读取文件为 {@link StructureTemplate}
     * 2) 通过 {@link ClientSchematicManager} 创建 {@link SchematicLevel} 并设为当前活跃关卡
     * 3) 将文件名写入 {@code targetStack} 的 {@code InitComponent.SCHEMATIC_NAME} 数据组件
     * 4) 向玩家显示提示并关闭界面
     */
    private void onSelect(String fileName) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return;
        
        try {
            ClientSchematicManager.getInstance().display(level, fileName);
            
            // 客户端不直接写入组件，改为发包给服务端写入
            PacketDistributor.sendToServer(new SchematicSelectPacket(fileName, this.hand));

            player.displayClientMessage(Component.literal("已选择蓝图: " + fileName), true);
            onClose();
            
        } catch (Exception e) {
            // 加载失败时的处理
            player.displayClientMessage(Component.literal("加载蓝图失败: " + fileName), true);
            e.printStackTrace();
        }
    }

    @Override
    /**
     * 渲染界面：
     * - 背景在 {@link #renderBackground} 中绘制；
     * - 此处绘制标题与空列表提示；
     * - 元素布局固定尺寸，未引入复杂的缩放或自适应逻辑。
     */
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        
        super.render(gfx, mouseX, mouseY, partialTick);
        // 面板背景
        
        gfx.drawString(this.font, this.title, left + 10, top + 10, 0xFFFFFF, false);
        if (nbtFiles.isEmpty()) {
            gfx.drawString(this.font, Component.literal("未找到 .nbt 文件"), left + 10, top + 40, 0xAAAAAA, false);
        }
        
        
    }
    @Override
    /** 简单的半透明矩形作为面板背景。 */
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(left, top, left + panelWidth, top + panelHeight, 0xCC000000);
    }

    @Override
    /** 非暂停界面，便于游戏内快速选择。 */
    public boolean isPauseScreen() {
        return false;
    }
}
