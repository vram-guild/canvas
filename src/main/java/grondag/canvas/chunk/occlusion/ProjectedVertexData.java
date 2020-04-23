package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_WIDTH;

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

	public static void setupVertex(final int[] data, final int baseIndex, final float x, final float y, final float z) {
		final Matrix4fExt mvpMatrixExt = Data.mvpMatrixExt;

		final float tx = mvpMatrixExt.a00() * x + mvpMatrixExt.a01() * y + mvpMatrixExt.a02() * z + mvpMatrixExt.a03();
		final float ty = mvpMatrixExt.a10() * x + mvpMatrixExt.a11() * y + mvpMatrixExt.a12() * z + mvpMatrixExt.a13();
		final float w = mvpMatrixExt.a30() * x + mvpMatrixExt.a31() * y + mvpMatrixExt.a32() * z + mvpMatrixExt.a33();

		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
		data[baseIndex + PV_Z] = Float.floatToRawIntBits(mvpMatrixExt.a20() * x + mvpMatrixExt.a21() * y + mvpMatrixExt.a22() * z + mvpMatrixExt.a23());
		data[baseIndex + PV_W] = Float.floatToRawIntBits(w);

		if (w != 0)  {
			final float iw = 1f / w;
			final int px = Math.round(tx * iw * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
			final int py = Math.round(ty * iw * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;

			data[baseIndex + PV_PX] = px;
			data[baseIndex + PV_PY] = py;
		}
	}

	public static int needsNearClip(final int[] data, final int baseIndex) {
		final float w = Float.intBitsToFloat(data[baseIndex + PV_W]);
		final float z = Float.intBitsToFloat(data[baseIndex + PV_Z]);

		//return w < 0.00001f ? 1 : 0;
		if (w == 0) {
			return 1;
		} else if (w > 0) {
			return (z > 0 && z <= w ) ? 0 : 1;
		} else {
			// w < 0
			return (z < 0 && z >= w) ? 0 : 1;
		}
	}
}
