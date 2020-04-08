package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.GUARD_HEIGHT;
import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.GUARD_SIZE;
import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.GUARD_WIDTH;
import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.HALF_PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.HALF_PRECISE_WIDTH;

import grondag.canvas.mixinterface.Matrix4fExt;

public final class ProjectedVertexData {
	private ProjectedVertexData() { }

	public static final int PV_PX = 0;
	public static final int PV_PY = 1;
	public static final int PV_X = 2;
	public static final int PV_Y = 3;
	public static final int PV_Z = 4;
	public static final int PV_W = 5;

	public static final int PROJECTED_VERTEX_STRIDE = 6;

	public static void setupVertex(final int[] data, final int baseIndex, final float x, final float y, final float z, Matrix4fExt mvpMatrixExt) {
		final float tx = mvpMatrixExt.a00() * x + mvpMatrixExt.a01() * y + mvpMatrixExt.a02() * z + mvpMatrixExt.a03();
		final float ty = mvpMatrixExt.a10() * x + mvpMatrixExt.a11() * y + mvpMatrixExt.a12() * z + mvpMatrixExt.a13();
		final float w = mvpMatrixExt.a30() * x + mvpMatrixExt.a31() * y + mvpMatrixExt.a32() * z + mvpMatrixExt.a33();
		final float iw = 1f / w;
		final int px = Math.round(tx * iw * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
		final int py = Math.round(ty * iw * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;

		data[baseIndex + PV_PX] = px;
		data[baseIndex + PV_PY] = py;
		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
		data[baseIndex + PV_Z] = Float.floatToRawIntBits(mvpMatrixExt.a20() * x + mvpMatrixExt.a21() * y + mvpMatrixExt.a22() * z + mvpMatrixExt.a23());
		data[baseIndex + PV_W] = Float.floatToRawIntBits(w);
	}

	public static int needsNearClip(final int[] data, final int baseIndex) {
		return Float.intBitsToFloat(data[baseIndex + PV_W]) <= 0 || Float.intBitsToFloat(data[baseIndex + PV_Z]) <= 0 ? 1 : 0;
	}

	public static void clipNear(final int[] data, int target, int internal, int external) {
		final float intX = Float.intBitsToFloat(data[internal + PV_X]);
		final float intY = Float.intBitsToFloat(data[internal + PV_Y]);
		final float intZ = Float.intBitsToFloat(data[internal + PV_Z]);

		final float extX = Float.intBitsToFloat(data[external + PV_X]);
		final float extY = Float.intBitsToFloat(data[external + PV_Y]);
		final float extZ = Float.intBitsToFloat(data[external + PV_Z]);

		// intersection point is the projection plane, at which point Z == 1
		// and w will be 0 but projection division isn't needed, so force output to W = 1
		// see https://www.cs.usfca.edu/~cruse/math202s11/homocoords.pdf

		// external Z  will be negative
		final float wt  = intZ / (intZ - extZ);

		// note again that projection division isn't needed
		final float x = (intX + (extX - intX) * wt);
		final float y = (intY + (extY - intY) * wt);

		data[target + PV_PX] = Math.round(x * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
		data[target + PV_PY] = Math.round(y * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;
		data[target + PV_X] = Float.floatToRawIntBits(x);
		data[target + PV_Y] = Float.floatToRawIntBits(y);
		data[target + PV_Z] = Float.floatToRawIntBits(1);
		data[target + PV_W] = Float.floatToRawIntBits(1);
	}

	public static int needsClipLowX(final int[] data, final int baseIndex) {
		return data[baseIndex + PV_PX] < -GUARD_SIZE ? 1 : 0;
	}

	public static void clipLowX(final int[] data, int target, int internal, int external) {
		// PERF: use fixed precision
		final float intX = data[internal + PV_PX];
		final float intY = data[internal + PV_PY];
		final float dx = data[external + PV_PX] - intX;
		final float dy = data[external + PV_PY] - intY;
		final float wt = (-GUARD_SIZE - intX) / dx;
		data[target + PV_PX] = -GUARD_SIZE; //Math.round(intX + wt * dx);
		data[target + PV_PY] = Math.round(intY + wt * dy);
	}

	public static int needsClipLowY(final int[] data, final int baseIndex) {
		return data[baseIndex + PV_PY] < -GUARD_SIZE ? 1 : 0;
	}

	public static void clipLowY(final int[] data, int target, int internal, int external) {
		final float intX = data[internal + PV_PX];
		final float intY = data[internal + PV_PY];
		final float dx = data[external + PV_PX] - intX;
		final float dy = data[external + PV_PY] - intY;
		final float wt = (-GUARD_SIZE - intY) / dy;
		data[target + PV_PX] = Math.round(intX + wt * dx);
		data[target + PV_PY] = -GUARD_SIZE; //Math.round(intY + wt * dy);
	}

	public static int needsClipHighX(final int[] data, final int baseIndex) {
		return data[baseIndex + PV_PX] > GUARD_WIDTH ? 1 : 0;
	}

	public static void clipHighX(final int[] data, int target, int internal, int external) {
		final float intX = data[internal + PV_PX];
		final float intY = data[internal + PV_PY];
		final float dx = data[external + PV_PX] - intX;
		final float dy = data[external + PV_PY] - intY;
		final float wt = (GUARD_WIDTH - intX) / dx;
		data[target + PV_PX] = GUARD_WIDTH; //Math.round(intX + wt * dx);
		data[target + PV_PY] = Math.round(intY + wt * dy);
	}

	public static int needsClipHighY(final int[] data, final int baseIndex) {
		return data[baseIndex + PV_PY] > GUARD_HEIGHT ? 1 : 0;
	}

	public static void clipHighY(final int[] data, int target, int internal, int external) {
		final float intX = data[internal + PV_PX];
		final float intY = data[internal + PV_PY];
		final float dx = data[external + PV_PX] - intX;
		final float dy = data[external + PV_PY] - intY;
		final float wt = (GUARD_HEIGHT - intY) / dy;
		data[target + PV_PX] = Math.round(intX + wt * dx);
		data[target + PV_PY] = GUARD_HEIGHT; //Math.round(intY + wt * dy);
	}
}
