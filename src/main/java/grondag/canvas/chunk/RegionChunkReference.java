package grondag.canvas.chunk;

import grondag.canvas.render.CanvasFrustum;

public class RegionChunkReference {

	public float cameraRelativeCenterX;
	public float cameraRelativeCenterY;
	public float cameraRelativeCenterZ;

	private int frustumVersion;
	private boolean frustumResult;

	public int chunkOriginX;
	public int chunkOriginZ;

	/**
	 * Assumes camera distance update has already happened
	 */
	public boolean isInFrustum(CanvasFrustum frustum) {
		final int v = frustum.viewVersion();

		if (v == frustumVersion) {
			return frustumResult;
		} else {
			frustumVersion = v;
			//  PERF: implement hierarchical tests with propagation of per-plane inside test results
			final boolean result = frustum.isChunkVisible(this);
			frustumResult = result;
			return result;
		}
	}

	public void setOrigin(int x, int z) {
		chunkOriginX = x;
		chunkOriginZ = z;
	}

	void updateCameraDistance(double cameraX, double cameraY, double cameraZ) {
		final float dx = (float) (chunkOriginX + 8 - cameraX);
		final float dy = (float) (128 - cameraY);
		final float dz = (float) (chunkOriginZ + 8 - cameraZ);
		cameraRelativeCenterX = dx;
		cameraRelativeCenterY = dy;
		cameraRelativeCenterZ = dz;
	}
}
