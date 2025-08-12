package com.dwinovo.piayn.lib.catnip.outliner;

import com.dwinovo.piayn.lib.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 物品渲染轮廓：在世界坐标某点渲染一个 ItemStack。
 * <p>
 * - 利用 {@code params.alpha} 作为缩放系数，可实现淡入淡出/缩放效果；
 * - 使用游戏内的 {@code ItemRenderer} 按固定展示（FIXED）进行渲染。
 */
public class ItemOutline extends Outline {

    protected Vec3 pos;
    protected ItemStack stack;

    /**
     * 构造一个物品渲染轮廓。
     * 
     * @param pos  渲染位置
     * @param stack 渲染的物品堆
     */
    public ItemOutline(Vec3 pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    @Override
    /**
     * 在给定位置渲染物品模型：
     * - 平移到世界位置减相机位置；
     * - 以 {@code params.alpha} 进行等比缩放；
     * - 使用 {@code ItemRenderer.render(..., ItemDisplayContext.FIXED, ...)} 绘制。
     */
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        Minecraft mc = Minecraft.getInstance();
        ms.pushPose();

        ms.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        ms.scale(params.alpha, params.alpha, params.alpha);

        mc.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false, ms,
                                buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                mc.getItemRenderer().getModel(stack, null, null, 0));

        ms.popPose();
    }
}