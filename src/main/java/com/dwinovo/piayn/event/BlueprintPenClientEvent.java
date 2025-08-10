package com.dwinovo.piayn.event;

import org.slf4j.Logger;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.client.resource.schem.SchemTexture;
import com.dwinovo.piayn.client.gui.screen.schem.SchematicSaveScreen;
import com.dwinovo.piayn.item.BlueprintPenItem;
import com.dwinovo.piayn.utils.RaycastHelper;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = PIAYN.MOD_ID,value = Dist.CLIENT)
public class BlueprintPenClientEvent {
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

    BindableTexture bindableTexture = () -> ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/schem/border.png");


    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof BlueprintPenItem)) return;

        // 计算当前目标格：
        // - Ctrl 按下：使用“视线 * range”的落点作为临时选中格，便于不指向方块时预览尺寸
        // - 非 Ctrl：使用游戏命中结果中的方块位置
        BlockPos lookingAt = null;
        if (isCtrlDown(mc)) {
            // 与 Create 一致：按下“激活工具”键（此处用Ctrl）时，使用投射点 eye + look * range
            float pt = AnimationTickHolder.getPartialTicks();
            Vec3 eye = player.getEyePosition(pt);
            Vec3 dir = player.getLookAngle();
            Vec3 target = eye.add(dir.scale(range));
            selectedPos = BlockPos.containing(target);
            lookingAt = selectedPos;
        } else {
            // 未按下时：用 RaycastHelper.rayTraceRange(75) 命中方块，并对垂直面做“上移一格”判定
            var trace = RaycastHelper.rayTraceRange(mc.level, player, 75);
            if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
                BlockPos hit = trace.getBlockPos();
                boolean replaceable = mc.level.getBlockState(hit)
                    .canBeReplaced(new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, trace)));
                if (trace.getDirection().getAxis().isVertical() && !replaceable) {
                    hit = hit.relative(trace.getDirection());
                }
                lookingAt = hit;
                selectedPos = hit;
            } else {
                selectedPos = null;
            }
        }

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

        AABB current = getCurrentSelectionBox(lookingAt, first, second);
        if (current != null) {
            Outliner.getInstance().chaseAABB(OUTLINE_SLOT, current)
                .colored(0x6886c5)
                .lineWidth(1 / 16f)
                .withFaceTextures(new SchemTexture(ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/schem/border.png")),
                        new SchemTexture(ResourceLocation.fromNamespaceAndPath(PIAYN.MOD_ID, "textures/schem/border.png")))
                .highlightFace(selectedFace);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (!(player.getMainHandItem().getItem() instanceof BlueprintPenItem)) return;
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
        if (mc == null || mc.player == null || mc.level == null || mc.screen != null)
            return;
        Player player = mc.player;
        if (!(player.getMainHandItem().getItem() instanceof BlueprintPenItem))
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
                String author = mc2.player != null ? mc2.player.getName().getString() : "Unknown";
                mc2.setScreen(new SchematicSaveScreen(mc2.level, firstPos, secondPos, author));
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
     * 计算“当前应渲染的选择盒”。
     * 规则与 Create 对齐：
     * - 当 second 为空：
     *   - first 为空：仅高亮 lookingAt（若存在）。
     *   - first 非空：若 lookingAt 存在，以 first 与 lookingAt 构盒；否则仅高亮 first。
     * - 当 second 非空：直接以 first、second 构盒。
     */
    private static AABB getCurrentSelectionBox(BlockPos lookingAt, BlockPos first, BlockPos second) {
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

    // 无需单独的射线求面方法：已对齐 Create 使用 RaycastHelper.rayTraceUntil 获取朝向

    // 使用原始 BlockPos 角点构造盒，不做 +1 扩展（与 Create 的 AABB(first, second) 语义对齐）
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
