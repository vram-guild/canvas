package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.HALF_PRECISE_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.PV_PX;
import static grondag.canvas.chunk.occlusion.Constants.PV_PY;
import static grondag.canvas.chunk.occlusion.Constants.PV_W;
import static grondag.canvas.chunk.occlusion.Constants.PV_X;
import static grondag.canvas.chunk.occlusion.Constants.PV_Y;
import static grondag.canvas.chunk.occlusion.Constants.PV_Z;

public final class ProjectedVertexData {
	private ProjectedVertexData() { }

	public static void setupVertex(final int baseIndex, final int x, final int y, final int z) {
		final int[] data = Data.vertexData;
		final Matrix4L mvpMatrix = Data.mvpMatrix;

		final float tx = mvpMatrix.transformVec4X(x, y, z) * Matrix4L.FLOAT_CONVERSION;
		final float ty = mvpMatrix.transformVec4Y(x, y, z) * Matrix4L.FLOAT_CONVERSION;
		final float w = mvpMatrix.transformVec4W(x, y, z) * Matrix4L.FLOAT_CONVERSION;

		data[baseIndex + PV_X] = Float.floatToRawIntBits(tx);
		data[baseIndex + PV_Y] = Float.floatToRawIntBits(ty);
		data[baseIndex + PV_Z] = Float.floatToRawIntBits(mvpMatrix.transformVec4Z(x, y, z) * Matrix4L.FLOAT_CONVERSION);
		data[baseIndex + PV_W] = Float.floatToRawIntBits(w);

		if (w != 0)  {
			final float iw = 1f / w;
			final int px = Math.round(tx * iw * HALF_PRECISE_WIDTH) + HALF_PRECISE_WIDTH;
			final int py = Math.round(ty * iw * HALF_PRECISE_HEIGHT) + HALF_PRECISE_HEIGHT;

			data[baseIndex + PV_PX] = px;
			data[baseIndex + PV_PY] = py;
		}
	}

	public static int needsNearClip(final int baseIndex) {
		final int[] data = Data.vertexData;
		final float w = Float.intBitsToFloat(data[baseIndex + PV_W]);
		final float z = Float.intBitsToFloat(data[baseIndex + PV_Z]);

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
