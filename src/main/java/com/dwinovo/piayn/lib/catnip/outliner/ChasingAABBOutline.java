package com.dwinovo.piayn.lib.catnip.outliner;

import org.joml.Vector4f;

import com.dwinovo.piayn.lib.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 可“追踪/插值”的 AABB 轮廓。
 * <p>
 * - 通过 {@link #target(AABB)} 指定目标包围盒；
 * - 每 tick 在 {@link #tick()} 中将当前 bb 以 0.5 系数插向目标；
 * - 渲染时对 prevBB→bb 进行帧间插值（使用 partial ticks），保证平滑。
 */
public class ChasingAABBOutline extends AABBOutline {

	AABB targetBB;
	AABB prevBB;

	public ChasingAABBOutline(AABB bb) {
		super(bb);
		prevBB = bb.inflate(0);
		targetBB = bb.inflate(0);
	}

	/** 设置追踪的目标包围盒。 */
	public void target(AABB target) {
		targetBB = target;
	}

	@Override
	/** 每 tick 推进一次：prev 记录旧值，bb 以 0.5 系数插向 target。 */
	public void tick() {
		prevBB = bb;
		setBounds(interpolateBBs(bb, targetBB, .5f));
	}

	@Override
	/** 渲染时对 prev→bb 进行帧间插值，从而在渲染上更加平滑。 */
	public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
		params.loadColor(colorTemp);
		Vector4f color = colorTemp;
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;
		renderBox(ms, buffer, camera, interpolateBBs(prevBB, bb, pt), color, lightmap, disableLineNormals);
	}

	/** 对两个 AABB 逐分量做线性插值。 */
	private static AABB interpolateBBs(AABB current, AABB target, float pt) {
		return new AABB(Mth.lerp(pt, current.minX, target.minX), Mth.lerp(pt, current.minY, target.minY),
				Mth.lerp(pt, current.minZ, target.minZ), Mth.lerp(pt, current.maxX, target.maxX),
				Mth.lerp(pt, current.maxY, target.maxY), Mth.lerp(pt, current.maxZ, target.maxZ));
	}

}

