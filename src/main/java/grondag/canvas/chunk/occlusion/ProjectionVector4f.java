package grondag.canvas.chunk.occlusion;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.MathHelper;

public class ProjectionVector4f extends Vector4f {
	protected float px;
	protected float py;
	protected int ix;
	protected int iy;
	protected int externalFlag;

	protected void calc() {
		px = getX() / getW();
		py = getY() / getW();
		ix = MathHelper.floor(TerrainOccluder.HALF_PIXEL_WIDTH + px * TerrainOccluder.HALF_PIXEL_WIDTH);
		iy = MathHelper.floor(TerrainOccluder.HALF_PIXEL_HEIGHT + py * TerrainOccluder.HALF_PIXEL_HEIGHT);
		externalFlag = getW() <= 0 ? 1 : 0;
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

	float px() {
		return px;
	}

	float py() {
		return py;
	}

	int ix() {
		return ix;
	}

	int iy() {
		return iy;
	}

	int externalFlag() {
		return externalFlag;
	}

	void interpolateClip(ProjectionVector4f internal, ProjectionVector4f external) {
		// external z will be negative
		final float wt  = internal.getZ() / (internal.getZ() - external.getZ());

		final float x = internal.getX() + (external.getX() - internal.getX()) * wt;
		final float y = internal.getY() + (external.getY() - internal.getY()) * wt;
		final float w = internal.getW() + (external.getW() - internal.getW()) * wt;

		set(x / w, y / w, 1, 1);
		calc();
	}
}
