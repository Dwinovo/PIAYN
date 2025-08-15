package com.dwinovo.piayn.client.renderer.catnip.outliner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.dwinovo.piayn.client.renderer.catnip.outliner.LineOutline.EndChasingLineOutline;
import com.dwinovo.piayn.client.renderer.catnip.outliner.Outline.OutlineParams;
import com.dwinovo.piayn.client.renderer.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 轮廓系统的门面与管理器（单例）。
 * <p>
 * - 负责创建/缓存/更新/渲染各类 {@link Outline} 实例（按 slot 键存取）。
 * - 提供便捷方法展示线段、AABB、追踪 AABB、方块簇与物品轮廓。
 * - 维护生命周期：每 tick 调用 {@link #tickOutlines()}，每帧调用
 *   {@link #renderOutlines(PoseStack, SuperRenderTypeBuffer, Vec3, float)}。
 * <p>
 * 集成要点：
 * - 客户端 Tick 事件中调用 {@code Outliner.getInstance().tickOutlines()}；
 * - 客户端渲染阶段中调用 {@code Outliner.getInstance().renderOutlines(...)}；
 * - 仅调用 show/chase 等 API 而不驱动上述两步，轮廓将不会显示或不会更新。
 */
public class Outliner {

	// Singleton

	private static final Outliner instance = new Outliner();

	public static Outliner getInstance() {
		return instance;
	}

	//

	public Outliner() {}

	private final Map<Object, OutlineEntry> outlines = Collections.synchronizedMap(new HashMap<>());
	private final Map<Object, OutlineEntry> outlinesView = Collections.unmodifiableMap(outlines);

	// Facade

	/**
	 * 直接展示一个自定义的 {@link Outline}，并返回其可链式配置的参数对象。
	 * 若同一 slot 之前已有轮廓，将被替换。
	 */
	public OutlineParams showOutline(Object slot, Outline outline) {
		outlines.put(slot, new OutlineEntry(outline));
		return outline.getParams();
	}

	/**
	 * 展示/刷新一条线段轮廓。首次调用会创建，之后重复调用仅更新端点并刷新寿命。
	 */
	public OutlineParams showLine(Object slot, Vec3 start, Vec3 end) {
		if (!outlines.containsKey(slot)) {
			LineOutline outline = new LineOutline();
			addOutline(slot, outline);
		}
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = 1;
		((LineOutline) entry.outline).set(start, end);
		return entry.outline.getParams();
	}

	/**
	 * 展示“末端追踪”的线段（带进度动画），可选锁定起点或终点。
	 */
	public OutlineParams endChasingLine(Object slot, Vec3 start, Vec3 end, float chasingProgress, boolean lockStart) {
		if (!outlines.containsKey(slot)) {
			EndChasingLineOutline outline = new EndChasingLineOutline(lockStart);
			addOutline(slot, outline);
		}
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = 1;
		((EndChasingLineOutline) entry.outline).setProgress(chasingProgress)
				.set(start, end);
		return entry.outline.getParams();
	}

	/**
	 * 立即显示 AABB（可指定保留 tick 数 ttl）。
	 * 会使用 {@link ChasingAABBOutline} 实现以便后续无缝切换为追踪。
	 */
	public OutlineParams showAABB(Object slot, AABB bb, int ttl) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot, ttl);
		outline.prevBB = outline.targetBB = outline.bb = bb;
		return outline.getParams();
	}

	public OutlineParams showAABB(Object slot, AABB bb) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot);
		outline.prevBB = outline.targetBB = outline.bb = bb;
		return outline.getParams();
	}

	/**
	 * 追踪 AABB：仅更新目标包围盒，位置将由 {@link ChasingAABBOutline#tick()} 渐进逼近。
	 */
	public OutlineParams chaseAABB(Object slot, AABB bb) {
		createAABBOutlineIfMissing(slot, bb);
		ChasingAABBOutline outline = getAndRefreshAABB(slot);
		outline.targetBB = bb;
		return outline.getParams();
	}

	/**
	 * 展示一个由多个方块组成的“合并轮廓”。
	 * 面需要通过 {@code params.withFaceTexture(...)} 指定贴图后才会渲染。
	 */
	public OutlineParams showCluster(Object slot, Iterable<BlockPos> selection) {
		BlockClusterOutline outline = new BlockClusterOutline(selection);
		addOutline(slot, outline);
		return outline.getParams();
	}

	//

	/** 在某个世界坐标显示一个物品（使用 ItemRenderer 渲染）。 */
	public OutlineParams showItem(Object slot, Vec3 pos, ItemStack stack) {
		ItemOutline outline = new ItemOutline(pos, stack);
		OutlineEntry entry = new OutlineEntry(outline);
		outlines.put(slot, entry);
		return entry.getOutline().getParams();
	}

	/**
	 * 保持指定 slot 的轮廓处于“存活”状态（重置移除计时）。
	 * 可在每帧编辑时调用，避免其进入淡出阶段。
	 */
	public void keep(Object slot) {
		if (outlines.containsKey(slot))
			outlines.get(slot).ticksTillRemoval = 1;
	}

	/** 立即移除指定 slot 的轮廓（无渐隐）。 */
	public void remove(Object slot) {
		outlines.remove(slot);
	}

	/**
	 * 取回指定 slot 的参数以继续链式编辑。若存在则同时 keep 一次确保不淡出。
	 */
	public Optional<OutlineParams> edit(Object slot) {
		keep(slot);
		if (outlines.containsKey(slot))
			return Optional.of(outlines.get(slot)
					.getOutline()
					.getParams());
		return Optional.empty();
	}

	public Map<Object, OutlineEntry> getOutlines() {
		return outlinesView;
	}

	// Utility

	private void addOutline(Object slot, Outline outline) {
		outlines.put(slot, new OutlineEntry(outline));
	}

	private void createAABBOutlineIfMissing(Object slot, AABB bb) {
		if (!outlines.containsKey(slot) || !(outlines.get(slot).outline instanceof AABBOutline)) {
			ChasingAABBOutline outline = new ChasingAABBOutline(bb);
			addOutline(slot, outline);
		}
	}

	private ChasingAABBOutline getAndRefreshAABB(Object slot) {
		return getAndRefreshAABB(slot, 1);
	}

	private ChasingAABBOutline getAndRefreshAABB(Object slot, int ttl) {
		OutlineEntry entry = outlines.get(slot);
		entry.ticksTillRemoval = ttl;
		return (ChasingAABBOutline) entry.getOutline();
	}

	// Maintenance

	/**
	 * 应在“客户端 Tick”阶段调用：
	 * - 递减所有轮廓的生存计数，进入/推进淡出；
	 * - 调用每个轮廓的 {@link Outline#tick()} 实现其内部动画（如追踪）。
	 */
	public void tickOutlines() {
		Iterator<OutlineEntry> iterator = outlines.values()
				.iterator();
		while (iterator.hasNext()) {
			OutlineEntry entry = iterator.next();
			entry.tick();
			if (!entry.isAlive())
				iterator.remove();
		}
	}

	/**
	 * 应在“渲染阶段”调用以绘制所有当前轮廓。
	 * - 根据是否处于淡出期为 {@code params.alpha} 计算平滑插值（立方曲线以更柔和）。
	 * - alpha 非常小时（< 1/8）跳过渲染以减少开销。
	 */
	public void renderOutlines(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
		outlines.forEach((key, entry) -> {
			Outline outline = entry.getOutline();
			OutlineParams params = outline.getParams();
			params.alpha = 1;
			if (entry.isFading()) {
				int prevTicks = entry.ticksTillRemoval + 1;
				float fadeticks = OutlineEntry.FADE_TICKS;
				float lastAlpha = prevTicks >= 0 ? 1 : 1 + (prevTicks / fadeticks);
				float currentAlpha = 1 + (entry.ticksTillRemoval / fadeticks);
				float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);

				params.alpha = alpha * alpha * alpha;
				if (params.alpha < 1 / 8f)
					return;
			}
			outline.render(ms, buffer, camera, pt);
		});
	}

	/**
	 * 轮廓条目的内部结构：包含轮廓实例与剩余 tick。
	 * 约定：当 {@code ticksTillRemoval < 0} 时进入淡出阶段，持续 FADE_TICKS 帧。
	 */
	public static class OutlineEntry {
		public static final int FADE_TICKS = 8;

		private final Outline outline;
		private int ticksTillRemoval = 1;

		public OutlineEntry(Outline outline) {
			this.outline = outline;
		}

		public Outline getOutline() {
			return outline;
		}

		public int getTicksTillRemoval() {
			return ticksTillRemoval;
		}

		public boolean isAlive() {
			return ticksTillRemoval >= -FADE_TICKS;
		}

		public boolean isFading() {
			return ticksTillRemoval < 0;
		}

		/** 递减剩余寿命并推进内部动画。*/
		public void tick() {
			ticksTillRemoval--;
			outline.tick();
		}
	}

}