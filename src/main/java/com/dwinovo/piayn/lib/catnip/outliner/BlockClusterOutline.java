package com.dwinovo.piayn.lib.catnip.outliner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.dwinovo.piayn.lib.catnip.data.Iterate;
import com.dwinovo.piayn.lib.catnip.render.BindableTexture;
import com.dwinovo.piayn.lib.catnip.render.PonderRenderTypes;
import com.dwinovo.piayn.lib.catnip.render.SuperRenderTypeBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;




import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;

/**
 * 方块簇轮廓（多方块的外表面与边缘合并渲染）。
 * <p>
 * - 将内部相邻方块的内接面剔除，仅渲染可见外表面与轮廓边缘，减少重复绘制。
 * - 面可使用半透明贴图（需设置 {@code OutlineParams.faceTexture}），边缘用实心轮廓线（有厚度）。
 * - 通过一个锚点 {@code anchor} 作为局部坐标原点，提高大区域下的浮点精度。
 */
public class BlockClusterOutline extends Outline {

	private final Cluster cluster;

	protected final Vector3f pos0Temp = new Vector3f();
	protected final Vector3f pos1Temp = new Vector3f();
	protected final Vector3f pos2Temp = new Vector3f();
	protected final Vector3f pos3Temp = new Vector3f();
	protected final Vector3f normalTemp = new Vector3f();
	protected final Vector3f originTemp = new Vector3f();

	public BlockClusterOutline(Iterable<BlockPos> positions) {
		cluster = new Cluster();
		positions.forEach(cluster::include);
	}

	@Override
	/** 主渲染入口：先画面（若有贴图），再画边。 */
	public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
		params.loadColor(colorTemp);
		Vector4f color = colorTemp;
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;

		renderFaces(ms, buffer, camera, pt, color, lightmap);
		renderEdges(ms, buffer, camera, pt, color, lightmap, disableLineNormals);
	}

	/**
	 * 渲染所有可见外表面（需要设置 {@code faceTexture}，否则跳过）。
	 * 使用晚期缓冲（late buffer）+ 半透明 RenderType，保证先后顺序与透明度正确。
	 */
	protected void renderFaces(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap) {
		BindableTexture faceTexture = params.faceTexture;
		if (faceTexture == null)
			return;
		if (cluster.isEmpty())
			return;

		ms.pushPose();
		ms.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y,
				cluster.anchor.getZ() - camera.z);

		PoseStack.Pose pose = ms.last();
		RenderType renderType = PonderRenderTypes.outlineTranslucent(faceTexture.getLocation(), true);
		VertexConsumer consumer = buffer.getLateBuffer(renderType);

		cluster.visibleFaces.forEach((face, axisDirection) -> {
			Direction direction = Direction.get(axisDirection, face.axis);
			BlockPos pos = face.pos;
			if (axisDirection == AxisDirection.POSITIVE)
				pos = pos.relative(direction.getOpposite());
			bufferBlockFace(pose, consumer, pos, direction, color, lightmap);
		});

		ms.popPose();
	}

	/**
	 * 渲染所有可见边缘（以具有厚度的线段表示）。
	 * 使用早/默认缓冲（solid）确保与面正确排序。
	 */
	protected void renderEdges(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap, boolean disableNormals) {
		float lineWidth = params.getLineWidth();
		if (lineWidth == 0)
			return;
		if (cluster.isEmpty())
			return;

		ms.pushPose();
		ms.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y,
				cluster.anchor.getZ() - camera.z);

		PoseStack.Pose pose = ms.last();
		VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());

		cluster.visibleEdges.forEach(edge -> {
			BlockPos pos = edge.pos;
			Vector3f origin = originTemp;
			origin.set(pos.getX(), pos.getY(), pos.getZ());
			Direction direction = Direction.get(AxisDirection.POSITIVE, edge.axis);
			bufferCuboidLine(pose, consumer, origin, direction, 1, lineWidth, color, lightmap, disableNormals);
		});

		ms.popPose();
	}

	/**
	 * 计算一个单位方块在不同朝向下的四个顶点与法线（用于贴图 UV 与法线光照）。
	 */
	public static void loadFaceData(Direction face, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector3f normal) {
		switch (face) {
			case DOWN -> {
				// 0 1 2 3
				pos0.set(0, 0, 1);
				pos1.set(0, 0, 0);
				pos2.set(1, 0, 0);
				pos3.set(1, 0, 1);
				normal.set(0, -1, 0);
			}
			case UP -> {
				// 4 5 6 7
				pos0.set(0, 1, 0);
				pos1.set(0, 1, 1);
				pos2.set(1, 1, 1);
				pos3.set(1, 1, 0);
				normal.set(0, 1, 0);
			}
			case NORTH -> {
				// 7 2 1 4
				pos0.set(1, 1, 0);
				pos1.set(1, 0, 0);
				pos2.set(0, 0, 0);
				pos3.set(0, 1, 0);
				normal.set(0, 0, -1);
			}
			case SOUTH -> {
				// 5 0 3 6
				pos0.set(0, 1, 1);
				pos1.set(0, 0, 1);
				pos2.set(1, 0, 1);
				pos3.set(1, 1, 1);
				normal.set(0, 0, 1);
			}
			case WEST -> {
				// 4 1 0 5
				pos0.set(0, 1, 0);
				pos1.set(0, 0, 0);
				pos2.set(0, 0, 1);
				pos3.set(0, 1, 1);
				normal.set(-1, 0, 0);
			}
			case EAST -> {
				// 6 3 2 7
				pos0.set(1, 1, 1);
				pos1.set(1, 0, 1);
				pos2.set(1, 0, 0);
				pos3.set(1, 1, 0);
				normal.set(1, 0, 0);
			}
		}
	}

	public static void addPos(float x, float y, float z, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3) {
		pos0.add(x, y, z);
		pos1.add(x, y, z);
		pos2.add(x, y, z);
		pos3.add(x, y, z);
	}

	/**
	 * 在局部坐标中将一个方块在某个朝向上的四边形写入缓冲，带少量偏移（1/128）以避免 Z-fighting。
	 */
	protected void bufferBlockFace(PoseStack.Pose pose, VertexConsumer consumer, BlockPos pos, Direction face, Vector4f color, int lightmap) {
		Vector3f pos0 = pos0Temp;
		Vector3f pos1 = pos1Temp;
		Vector3f pos2 = pos2Temp;
		Vector3f pos3 = pos3Temp;
		Vector3f normal = normalTemp;

		loadFaceData(face, pos0, pos1, pos2, pos3, normal);
		addPos(pos.getX() + face.getStepX() * 1 / 128f,
				pos.getY() + face.getStepY() * 1 / 128f,
				pos.getZ() + face.getStepZ() * 1 / 128f,
				pos0, pos1, pos2, pos3);

		bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normal);
	}

	/**
	 * 用于收集/合并可见面与可见边的数据结构。
	 * <p>
	 * - visibleFaces: 每个外表面（位置+面法线）映射一个朝向符号（正/负）。
	 * - visibleEdges: 每条外边（位置+轴）。
	 */
	private static class Cluster {

		private BlockPos anchor;
		private Map<MergeEntry, AxisDirection> visibleFaces;
		private Set<MergeEntry> visibleEdges;

		public Cluster() {
			visibleEdges = new HashSet<>();
			visibleFaces = new HashMap<>();
		}

		public boolean isEmpty() {
			return anchor == null;
		}

		public void include(BlockPos pos) {
			if (anchor == null)
				anchor = pos;

			pos = pos.subtract(anchor);

			// 6 FACES
			for (Axis axis : Iterate.axes) {
				Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
				for (int offset : Iterate.zeroAndOne) {
					MergeEntry entry = new MergeEntry(axis, pos.relative(direction, offset));
					if (visibleFaces.remove(entry) == null)
						visibleFaces.put(entry, offset == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
				}
			}

			// 12 EDGES
			for (Axis axis : Iterate.axes) {
				for (Axis axis2 : Iterate.axes) {
					if (axis == axis2)
						continue;
					for (Axis axis3 : Iterate.axes) {
						if (axis == axis3)
							continue;
						if (axis2 == axis3)
							continue;

						Direction direction = Direction.get(AxisDirection.POSITIVE, axis2);
						Direction direction2 = Direction.get(AxisDirection.POSITIVE, axis3);

						for (int offset : Iterate.zeroAndOne) {
							BlockPos entryPos = pos.relative(direction, offset);
							for (int offset2 : Iterate.zeroAndOne) {
								entryPos = entryPos.relative(direction2, offset2);
								MergeEntry entry = new MergeEntry(axis, entryPos);
								if (!visibleEdges.remove(entry))
									visibleEdges.add(entry);
							}
						}
					}

					break;
				}
			}

		}

	}

	private static class MergeEntry {

		private Axis axis;
		private BlockPos pos;

		public MergeEntry(Axis axis, BlockPos pos) {
			this.axis = axis;
			this.pos = pos;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof MergeEntry))
				return false;

			MergeEntry other = (MergeEntry) o;
			return this.axis == other.axis && this.pos.equals(other.pos);
		}

		@Override
		public int hashCode() {
			return this.pos.hashCode() * 31 + axis.ordinal();
		}
	}

}

