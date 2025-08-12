package com.dwinovo.piayn.lib.catnip.outliner;

import org.joml.Vector3d;
import org.joml.Vector4f;

import com.dwinovo.piayn.lib.catnip.render.PonderRenderTypes;
import com.dwinovo.piayn.lib.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 线段轮廓（以“细长方体”实现厚度）。
 * <p>
 * - 使用 {@link Outline#bufferCuboidLine} 写入具有宽度的线段几何。
 * - 颜色、线宽、光照等由 {@link OutlineParams} 控制。
 */
public class LineOutline extends Outline {

	protected final Vector3d start = new Vector3d(0, 0, 0);
	protected final Vector3d end = new Vector3d(0, 0, 0);

	/** 设置线段的起止点（Vector3d） */
	public LineOutline set(Vector3d start, Vector3d end) {
		this.start.set(start.x, start.y, start.z);
		this.end.set(end.x, end.y, end.z);
		return this;
	}

	/** 设置线段的起止点（Vec3） */
	public LineOutline set(Vec3 start, Vec3 end) {
		this.start.set(start.x, start.y, start.z);
		this.end.set(end.x, end.y, end.z);
		return this;
	}

	@Override
	/** 渲染线段（当线宽为 0 时跳过）。 */
	public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
		float width = params.getLineWidth();
		if (width == 0)
			return;

		VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());
		params.loadColor(colorTemp);
		Vector4f color = colorTemp;
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;
		renderInner(ms, consumer, camera, pt, width, color, lightmap, disableLineNormals);
	}

	/** 实际写入线段几何的内部方法（可被子类覆盖）。 */
	protected void renderInner(PoseStack ms, VertexConsumer consumer, Vec3 camera, float pt, float width,
							   Vector4f color, int lightmap, boolean disableNormals) {
		bufferCuboidLine(ms, consumer, camera, start, end, width, color, lightmap, disableNormals);
	}

	/**
	 * 末端追踪线段（支持“从起点/终点向目标逐渐靠拢”的动画效果）。
	 * <p>
	 * progress ∈ [0,1]，lockStart=true 时表示 <start→end> 缩短到 end；
	 * 否则反向压缩（等价于从 end 向 start 延伸）。
	 */
	public static class EndChasingLineOutline extends LineOutline {
		private float progress = 0;
		private float prevProgress = 0;
		private boolean lockStart;

		private final Vector3d startTemp = new Vector3d(0, 0, 0);

		public EndChasingLineOutline(boolean lockStart) {
			this.lockStart = lockStart;
		}

		/** 设置进度（会记录上次进度用于帧间插值）。 */
		public EndChasingLineOutline setProgress(float progress) {
			prevProgress = this.progress;
			this.progress = progress;
			return this;
		}

		@Override
		/** 根据 progress 与 pt 插值，动态改变线段的一端位置后再写入几何。 */
		protected void renderInner(PoseStack ms, VertexConsumer consumer, Vec3 camera, float pt, float width,
							   Vector4f color, int lightmap, boolean disableNormals) {
			float distanceToTarget = Mth.lerp(pt, prevProgress, progress);

			Vector3d end;
			if (lockStart) {
				end = this.start;
			} else {
				end = this.end;
				distanceToTarget = 1 - distanceToTarget;
			}

			Vector3d start = this.startTemp;
			double x = (this.start.x - end.x) * distanceToTarget + end.x;
			double y = (this.start.y - end.y) * distanceToTarget + end.y;
			double z = (this.start.z - end.z) * distanceToTarget + end.z;
			start.set((float) x, (float) y, (float) z);
			bufferCuboidLine(ms, consumer, camera, start, end, width, color, lightmap, disableNormals);
		}
	}

}
