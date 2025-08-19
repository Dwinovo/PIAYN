package com.dwinovo.piayn.event.schematic;

import org.slf4j.Logger;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.gui.screen.schematic.SchematicSaveScreen;
// import removed: AnimationTickHolder now used via ClientUtils
import com.dwinovo.piayn.client.renderer.catnip.outliner.Outliner;
import com.dwinovo.piayn.client.renderer.catnip.render.BindableTexture;
import com.dwinovo.piayn.item.SchematicPenItem;
import com.dwinovo.piayn.utils.ClientUtils;
import com.dwinovo.piayn.utils.RaycastHelper;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
// imports removed: contexts moved to ClientUtils
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 蓝图笔（客户端）交互与可视化事件：
 *
 * 职责：
 * - 在客户端本地维护选择状态（firstPos/secondPos/selectedPos/selectedFace/range）；
 * - 在 AFTER_PARTICLES 阶段用 Outliner 渲染选区轮廓与高亮面；
 * - 监听滚轮事件，沿当前选中面缩放选区尺寸；
 * - 监听右键：Shift 清空；两次点击设置 first/second；已有 second 时打开保存界面。
 *
 * 设计要点：
 * - 完全客户端本地状态，不写入服务端，符合蓝图框选的预览/编辑性质；
 * - Ctrl 下使用 eye + look * range 的投射点便于“空中”预览尺寸；
 * - 命中垂直面且不可替换时，上移一格避免覆盖；
 * - selectedFace 用于确定滚轮缩放朝向，若相机位于盒内则滚轮方向取反以符合直觉；
 * - Outliner 使用“槽位句柄”以实现追踪更新与统一渲染。
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID,value = Dist.CLIENT)
public class SchematicPenClientEvent {
    public static final Logger LOGGER = LogUtils.getLogger();
    // 客户端侧选择状态（与 Create 的 Handler 思路一致）
    private static BlockPos firstPos;
    private static BlockPos secondPos;
    private static BlockPos selectedPos;
    private static Direction selectedFace;
    private static int range = 10;

    // Outliner 渲染用的槽位句柄（同一槽位可被 "追踪" 更新）
    private static final Object OUTLINE_SLOT = new Object();
    // 当前“指向或投射”的方块位置（非必然写入到物品的 first/second，仅用于渲染辅助）
    // 注：selectedPos/selectedFace/range 已在上方声明为客户端本地状态

    private static final BindableTexture FACE_TEXTURE = () -> ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/schematic/border.png");
    private static final BindableTexture HIGHLIGHT_FACE_TEXTURE = () -> ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/schematic/hightlignt_border.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof SchematicPenItem)) return;

        // 计算当前目标格（统一封装在 ClientUtils）：
        BlockPos lookingAt = ClientUtils.getLookingAt(mc, player, range);
        selectedPos = lookingAt; // 维持本地状态，供后续点击等逻辑使用

        // 根据已选 first/second 和 lookingAt 计算当前渲染盒（使用客户端本地状态）
        BlockPos first = firstPos;
        BlockPos second = secondPos;

        // 当存在完整区域时计算一个“选中面”，用于滚轮沿面缩放：
        
        selectedFace = null;
        if (first != null && second != null) {
            AABB bb = makeBox(first, second).expandTowards(1, 1, 1).inflate(.45f);
            Vec3 projectedView = mc.gameRenderer.getMainCamera().getPosition();
            boolean inside = bb.contains(projectedView);
            var result = RaycastHelper.rayTraceUntil(player, 70, pos -> {
                Vec3 c = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                return inside ^ bb.contains(c);
            });
            selectedFace = (result == null || result.missed()) ? null : (inside ? result.getFacing().getOpposite() : result.getFacing());
        }

        AABB current = getSelectionBox(lookingAt, first, second);
        if (current != null) {
            Outliner.getInstance().chaseAABB(OUTLINE_SLOT, current)
                .colored(0x6886c5)
                .lineWidth(1 / 16f)
                .withFaceTextures(FACE_TEXTURE,HIGHLIGHT_FACE_TEXTURE)
                .highlightFace(selectedFace);
        }
    
    }
    
    @SubscribeEvent
    /**
     * 滚轮缩放逻辑：
     * - Ctrl + 滚轮生效；
     * - 未设置 secondPos 时，调整“投射距离 range”以改变 Ctrl 预览长度；
     * - 已有完整区域且存在 selectedFace 时，沿该面方向扩大/收缩 AABB；
     * - 若相机在盒内，则滚轮方向取反，符合“朝向相机”直觉；
     * - 调整结果回写到 firstPos/secondPos；
     * - 事件被取消以阻止原生缩放。
     */
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof SchematicPenItem)) return;
        if (!isCtrlDown(mc)) return;

        double delta = event.getScrollDeltaY();
        if (secondPos == null) {
            range = (int) Mth.clamp(range + delta, 1, 100);
            event.setCanceled(true);
            return;
        }
        if (selectedFace == null) return;

        // 基于两个端点创建当前包围盒
        AABB bb = makeBox(firstPos, secondPos);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (bb.contains(camera)) delta *= -1; // 视点在盒内，反向滚动以符合直觉

        int intDelta = (int) (delta > 0 ? Math.ceil(delta) : Math.floor(delta));
        if (intDelta == 0) return;

        var vec = selectedFace.getNormal();
        int x = vec.getX() * intDelta;
        int y = vec.getY() * intDelta;
        int z = vec.getZ() * intDelta;

        // 根据面的朝向决定移动/收缩的方向与极性
        AxisDirection dir = selectedFace.getAxisDirection();
        if (dir == AxisDirection.NEGATIVE) {
            bb = bb.move(-x, -y, -z);
        }

        double maxX = Math.max(bb.maxX - x * dir.getStep(), bb.minX);
        double maxY = Math.max(bb.maxY - y * dir.getStep(), bb.minY);
        double maxZ = Math.max(bb.maxZ - z * dir.getStep(), bb.minZ);
        AABB resized = new AABB(bb.minX, bb.minY, bb.minZ, maxX, maxY, maxZ);

        // 回写到客户端本地状态，完全模仿 Create 的 Handler 本地维护
        firstPos = BlockPos.containing(resized.minX, resized.minY, resized.minZ);
        secondPos = BlockPos.containing(resized.maxX, resized.maxY, resized.maxZ);
        event.setCanceled(true);
    }

    // 监听鼠标右键（按下），在客户端本地设置/清空选择点，
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.getAction() != GLFW.GLFW_PRESS)
            return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc == null || player == null || mc.level == null || mc.screen != null)
            return;
        
        if (!(player.getMainHandItem().getItem() instanceof SchematicPenItem))
            return;

        // Shift 清空
        if (player.isShiftKeyDown()) {
            firstPos = null;
            secondPos = null;
            player.displayClientMessage(Component.literal("§e[蓝图笔] 已清空选择"), true);
            event.setCanceled(true);
            return;
        }

        // 若已有 second，则打开保存 GUI
        if (secondPos != null) {
            Minecraft mc2 = Minecraft.getInstance();
            if (mc2.level != null && firstPos != null) {
                mc2.setScreen(new SchematicSaveScreen(mc2.level, firstPos, secondPos));
            }
            event.setCanceled(true);
            return;
        }

        if (selectedPos == null) {
            player.displayClientMessage(Component.literal("§c[蓝图笔] 没有可用的目标"), true);
            event.setCanceled(true);
            return;
        }

        if (firstPos == null) {
            firstPos = selectedPos;
            player.displayClientMessage(Component.literal("§e[蓝图笔] 第一个点已设置: " + firstPos), true);
        } else {
            secondPos = selectedPos;
            player.displayClientMessage(Component.literal("§e[蓝图笔] 第二个点已设置: " + secondPos), true);
        }
        event.setCanceled(true);
    }

    /**
     * 检测是否按下 Ctrl（左右任意）。
     * 使用 GLFW 直接查询窗口键态，避免与键位映射冲突。
     */
    private static boolean isCtrlDown(Minecraft mc) {
        long handle = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    /**
     * 基于 first/second/lookingAt 生成当前可视化 AABB：
     * - 未设置任何点：仅在 lookingAt 非空时显示 1x1x1 的预览方块；
     * - 仅有 first：与 lookingAt 形成对角；
     * - 有 first+second：返回封闭盒。
     */
    private static AABB getSelectionBox(BlockPos lookingAt, BlockPos first, BlockPos second) {
        if (second == null) {
            if (first == null)
                return lookingAt == null ? null : new AABB(lookingAt.getX(), lookingAt.getY(), lookingAt.getZ(), lookingAt.getX() + 1, lookingAt.getY() + 1, lookingAt.getZ() + 1);
            return lookingAt == null ? new AABB(first.getX(), first.getY(), first.getZ(), first.getX() + 1, first.getY() + 1, first.getZ() + 1)
                : new AABB(Math.min(first.getX(), lookingAt.getX()), Math.min(first.getY(), lookingAt.getY()), Math.min(first.getZ(), lookingAt.getZ()),
                    Math.max(first.getX(), lookingAt.getX()) + 1, Math.max(first.getY(), lookingAt.getY()) + 1, Math.max(first.getZ(), lookingAt.getZ()) + 1);
        }
        return new AABB(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
            Math.max(first.getX(), second.getX()) + 1, Math.max(first.getY(), second.getY()) + 1, Math.max(first.getZ(), second.getZ()) + 1);
    }

    /**
     * 根据两个端点生成 AABB（不+1 的闭区间盒，后续用 expand/inflate/withFaceTextures 进行可视化调整）。
     */
    private static AABB makeBox(BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
