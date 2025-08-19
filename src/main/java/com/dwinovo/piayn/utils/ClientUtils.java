package com.dwinovo.piayn.utils;

import com.dwinovo.piayn.client.renderer.catnip.animation.AnimationTickHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class ClientUtils {
    /**
     * 统一的“当前指向/投射”的方块位置计算：
     * - 若 mc.level 为空：返回玩家脚下一格上方；
     * - Ctrl 按下：返回 eye + look * range 的落点格；
     * - 否则：rayTraceRange 命中方块；若命中垂直面且不可替换，上移一格；未命中则返回脚下上方。
     */
    public static BlockPos getLookingAt(Minecraft mc, Player player, int range) {
        if (mc == null || player == null) return null;
        if (mc.level == null) return player.blockPosition().above();

        if (isCtrlDown(mc)) {
            float pt = AnimationTickHolder.getPartialTicks();
            Vec3 eye = player.getEyePosition(pt);
            Vec3 dir = player.getLookAngle();
            Vec3 target = eye.add(dir.scale(range));
            return BlockPos.containing(target);
        }

        var trace = RaycastHelper.rayTraceRange(mc.level, player, 75);
        if (trace != null && trace.getType() == HitResult.Type.BLOCK) {
            BlockPos hit = trace.getBlockPos();
            boolean replaceable = mc.level.getBlockState(hit)
                .canBeReplaced(new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, trace)));
            if (trace.getDirection().getAxis().isVertical() && !replaceable) {
                hit = hit.relative(trace.getDirection());
            }
            return hit;
        }
        return player.blockPosition().above();
    }

    private static boolean isCtrlDown(Minecraft mc) {
        long handle = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}
