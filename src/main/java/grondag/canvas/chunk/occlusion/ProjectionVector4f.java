package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.TerrainOccluder.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.HALF_PRECISION_HEIGHT;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.HALF_PRECISION_WIDTH;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;

public class ProjectionVector4f extends Vector4f {
	//	protected float px;
	//	protected float py;
	protected int ix;
	protected int iy;
	protected int needsNearClip;

	protected void calc() {
		final float px = getX() / getW() * HALF_PRECISION_WIDTH;
		final float py = getY() / getW() * HALF_PRECISION_HEIGHT;
		ix = Math.round(px) + HALF_PRECISION_WIDTH;
		iy = Math.round(py) + HALF_PRECISION_HEIGHT;
		needsNearClip = getW() <= 0 ? 1 : 0;
	}

	@Override
	public void set(float f, float g, float h, float i) {
		super.set(f, g, h, i);
	}

	@Override
	public void transform(Matrix4f matrix4f) {
		super.transform(matrix4f);
		calc();
	}

	//	float px() {
	//		return px;
	//	}
	//
	//	float py() {
	//		return py;
	//	}

	int ix() {
		return ix;
	}

	int iy() {
		return iy;
	}

	int needsNearClip() {
		return needsNearClip;
	}

	void clipNear(ProjectionVector4f internal, ProjectionVector4f external) {

		// intersection point is zero, so no need to subtract it
		final float wt  = (0 - internal.getZ()) / (external.getZ() - internal.getZ());

		final float x = internal.getX() + (external.getX() - internal.getX()) * wt;
		final float y = internal.getY() + (external.getY() - internal.getY()) * wt;
		final float w = internal.getW() + (external.getW() - internal.getW()) * wt;
		set(x / w, y / w, 1, 1);
		calc();
	}

	int needsClipLowX() {
		return ix < -GUARD_SIZE ? 1 : 0;
	}

	void clipLowX(ProjectionVector4f internal, ProjectionVector4f external) {
		final float dx = external.ix - internal.ix;
		final float dy = external.iy - internal.iy;
		final float wt = (-GUARD_SIZE - internal.ix) / dx;
		ix = Math.round(internal.ix + wt * dx);
		iy = Math.round(internal.iy + wt * dy);
	}

	int needsClipLowY() {
		return iy < -GUARD_SIZE ? 1 : 0;
	}

	void clipLowY(ProjectionVector4f internal, ProjectionVector4f external) {
		final float dx = external.ix - internal.ix;
		final float dy = external.iy - internal.iy;
		final float wt = (-GUARD_SIZE - internal.iy) / dy;
		ix = Math.round(internal.ix + wt * dx);
		iy = Math.round(internal.iy + wt * dy);
	}

	int needsClipHighX() {
		return ix > GUARD_WIDTH ? 1 : 0;
	}

	void clipHighX(ProjectionVector4f internal, ProjectionVector4f external) {
		final float dx = external.ix - internal.ix;
		final float dy = external.iy - internal.iy;
		final float wt = (GUARD_WIDTH - internal.ix) / dx;
		ix = Math.round(internal.ix + wt * dx);
		iy = Math.round(internal.iy + wt * dy);
	}

	int needsClipHighY() {
		return iy > GUARD_HEIGHT ? 1 : 0;
	}

	void clipHighY(ProjectionVector4f internal, ProjectionVector4f external) {
		final float dx = external.ix - internal.ix;
		final float dy = external.iy - internal.iy;
		final float wt = (GUARD_HEIGHT - internal.iy) / dy;
		ix = Math.round(internal.ix + wt * dx);
		iy = Math.round(internal.iy + wt * dy);
	}
}
