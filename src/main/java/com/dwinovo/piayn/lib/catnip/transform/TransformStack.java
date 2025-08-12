package com.dwinovo.piayn.lib.catnip.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 简单的变换栈实现，不依赖Flywheel
 * 提供链式调用的变换操作，直接操作PoseStack
 */
public class TransformStack {
    private final PoseStack poseStack;

    private TransformStack(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    /**
     * 创建TransformStack实例
     */
    public static TransformStack of(PoseStack poseStack) {
        return new TransformStack(poseStack);
    }

    /**
     * 平移变换
     */
    public TransformStack translate(double x, double y, double z) {
        poseStack.translate(x, y, z);
        return this;
    }

    public TransformStack translate(float x, float y, float z) {
        poseStack.translate(x, y, z);
        return this;
    }

    public TransformStack translate(Vector3f vec) {
        poseStack.translate(vec.x(), vec.y(), vec.z());
        return this;
    }

    /**
     * X轴旋转（弧度）
     */
    public TransformStack rotateX(float radians) {
        poseStack.mulPose(Axis.XP.rotation(radians));
        return this;
    }

    /**
     * Y轴旋转（弧度）
     */
    public TransformStack rotateY(float radians) {
        poseStack.mulPose(Axis.YP.rotation(radians));
        return this;
    }

    /**
     * Z轴旋转（弧度）
     */
    public TransformStack rotateZ(float radians) {
        poseStack.mulPose(Axis.ZP.rotation(radians));
        return this;
    }

    /**
     * X轴旋转（角度）
     */
    public TransformStack rotateXDegrees(float degrees) {
        return rotateX(degrees * Mth.DEG_TO_RAD);
    }

    /**
     * Y轴旋转（角度）
     */
    public TransformStack rotateYDegrees(float degrees) {
        return rotateY(degrees * Mth.DEG_TO_RAD);
    }

    /**
     * Z轴旋转（角度）
     */
    public TransformStack rotateZDegrees(float degrees) {
        return rotateZ(degrees * Mth.DEG_TO_RAD);
    }

    /**
     * 任意轴旋转（四元数）
     */
    public TransformStack rotate(Quaternionf quaternion) {
        poseStack.mulPose(quaternion);
        return this;
    }

    /**
     * 缩放变换
     */
    public TransformStack scale(float scale) {
        poseStack.scale(scale, scale, scale);
        return this;
    }

    public TransformStack scale(float x, float y, float z) {
        poseStack.scale(x, y, z);
        return this;
    }

    /**
     * 推入新的变换矩阵
     */
    public TransformStack pushPose() {
        poseStack.pushPose();
        return this;
    }

    /**
     * 弹出变换矩阵
     */
    public TransformStack popPose() {
        poseStack.popPose();
        return this;
    }

    /**
     * 获取底层的PoseStack
     */
    public PoseStack getPoseStack() {
        return poseStack;
    }

    /**
     * 围绕指定点旋转
     */
    public TransformStack rotateAround(Quaternionf quaternion, float x, float y, float z) {
        return translate(x, y, z)
                .rotate(quaternion)
                .translate(-x, -y, -z);
    }

    public TransformStack rotateAround(Quaternionf quaternion, Vector3f center) {
        return rotateAround(quaternion, center.x(), center.y(), center.z());
    }

    /**
     * 围绕中心点旋转（0.5, 0.5, 0.5）
     */
    public TransformStack rotateCentered(Quaternionf quaternion) {
        return rotateAround(quaternion, 0.5f, 0.5f, 0.5f);
    }

    public TransformStack rotateCenteredDegrees(float degrees, Axis axis) {
        return rotateCentered(axis.rotation(degrees * Mth.DEG_TO_RAD));
    }

    /**
     * 重置到单位矩阵（清除所有变换）
     */
    public TransformStack identity() {
        poseStack.setIdentity();
        return this;
    }
}
