package grondag.canvas.chunk.occlusion;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.MathHelper;

public class Lazy4f extends Vector4f {
	protected boolean dirty = true;
	protected float px;
	protected float py;
	protected int ix;
	protected int iy;
	protected int externalFlag;

	protected void calc() {
		if (dirty) {
			px = getX() / getW();
			py = getY() / getW();
			ix = MathHelper.floor(TerrainOccluder.HALF_WIDTH + px * TerrainOccluder.HALF_WIDTH);
			iy = MathHelper.floor(TerrainOccluder.HALF_HEIGHT + py * TerrainOccluder.HALF_HEIGHT);
			externalFlag = getW() <= 0 ? 1 : 0;
			dirty = false;
		}
	}

	@Override
	public void set(float f, float g, float h, float i) {
		super.set(f, g, h, i);
		dirty = true;
	}

	@Override
	public void transform(Matrix4f matrix4f) {
		super.transform(matrix4f);
		dirty = true;
	}

	float px() {
		calc();
		return px;
	}

	float py() {
		calc();
		return py;
	}

	int ix() {
		calc();
		return ix;
	}

	int iy() {
		calc();
		return iy;
	}

	int externalFlag() {
		calc();
		return externalFlag;
	}

	void interpolateClip(Lazy4f internal, Lazy4f external) {
		// external z will be negative
		final float wt  = internal.getZ() / (internal.getZ() - external.getZ());

		final float x = internal.getX() + (external.getX() - internal.getX()) * wt;
		final float y = internal.getY() + (external.getY() - internal.getY()) * wt;
		final float w = internal.getW() + (external.getW() - internal.getW()) * wt;

		set(x / w, y / w, 1, 1);
	}
}
