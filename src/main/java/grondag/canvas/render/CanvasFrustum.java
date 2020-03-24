package grondag.canvas.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.mixinterface.Matrix4fExt;

/**
 * Plane equation derivations based on:
 * "Fast Extraction of Viewing Frustum Planes from the World- View-Projection Matrix"
 * Gill Gribb, Klaus Hartmann
 * https://www.gamedevs.org/uploads/fast-extraction-viewing-frustum-planes-from-world-view-projection-matrix.pdf
 *
 * AABB test method based on work by Ville Miettinen
 * as described in Real-Time Rendering, Fourth Edition (Page 971). CRC Press.
 * Abbey, Duane C.; Haines, Eric; Hoffman, Naty.
 */
@Environment(EnvType.CLIENT)
public class CanvasFrustum extends Frustum {
	private final Matrix4f mvpMatrix = new Matrix4f();

	private Frustum testFrustum;

	private final float[] planes = new float[6 * PLANE_STRIDE];

	public CanvasFrustum() {
		super(dummyMatrix(), dummyMatrix());
	}

	private static Matrix4f dummyMatrix() {
		final Matrix4f dummt = new Matrix4f();
		dummt.loadIdentity();
		return dummt;
	}

	public void prepare(Matrix4f modelMatrix, Matrix4f projectionMatrix, Camera camera) {
		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);

		extractPlanes();

		testFrustum = new Frustum(modelMatrix, projectionMatrix);

		final Vec3d vec = camera.getPos();

		testFrustum.setPosition(vec.x, vec.y, vec.z);
	}

	// PERF: optimize for OBB
	@Override
	public boolean isVisible(Box box) {
		return testFrustum.isVisible(box);
	}

	public boolean isChunkVisible(BuiltRenderRegion region) {
		final float cx = region.cameraRelativeCenterX;
		final float cy = region.cameraRelativeCenterY;
		final float cz = region.cameraRelativeCenterZ;

		return planeAABBIntersect(cx, cy, cz, PLANE_NEAR) != OUTSIDE
				&& planeAABBIntersect(cx, cy, cz, PLANE_LEFT) != OUTSIDE
				&& planeAABBIntersect(cx, cy, cz, PLANE_RIGHT) != OUTSIDE
				&& planeAABBIntersect(cx, cy, cz, PLANE_TOP) != OUTSIDE
				&& planeAABBIntersect(cx, cy, cz, PLANE_BOTTOM) != OUTSIDE;
		//				&& planeAABBIntersect(cx, cy, cz, PLANE_FAR) != OUTSIDE;
	}

	private int planeAABBIntersect(float cx, float cy, float cz, int planeOffset) {
		final float[] planes = this.planes;
		final float e = planes[planeOffset + PLANE_EXTENT];
		final float s = cx * planes[planeOffset + PLANE_NORMAL_X]
				+ cy * planes[planeOffset + PLANE_NORMAL_Y]
						+ cz * planes[planeOffset + PLANE_NORMAL_Z]
								+ planes[planeOffset + PLANE_DISTANCE];

		if(s - e > 0) {
			return OUTSIDE;
		} else if(s + e < 0) {
			return INSIDE;
		} else {
			return INTERSECTING;
		}
	}

	private static final int OUTSIDE = 0;
	private static final int INSIDE = 1;
	private static final int INTERSECTING = 2;

	private static final int PLANE_NORMAL_X = 0;
	private static final int PLANE_NORMAL_Y = 1;
	private static final int PLANE_NORMAL_Z = 2;
	private static final int PLANE_DISTANCE = 3;
	private static final int PLANE_EXTENT = 4;
	private static final int PLANE_STRIDE = PLANE_EXTENT + 1;

	private static final int PLANE_LEFT = 0;
	private static final int PLANE_RIGHT = PLANE_STRIDE;
	private static final int PLANE_TOP = PLANE_STRIDE * 2;
	private static final int PLANE_BOTTOM = PLANE_STRIDE * 3;
	private static final int PLANE_NEAR = PLANE_STRIDE * 4;
	//	private static final int PLANE_FAR = PLANE_STRIDE * 5;

	private void normalizePlane(int planeOffset) {
		final float[] planes = this.planes;

		float a  = planes[planeOffset + PLANE_NORMAL_X];
		float b  = planes[planeOffset + PLANE_NORMAL_Y];
		float c  = planes[planeOffset + PLANE_NORMAL_Z];
		float d  = planes[planeOffset + PLANE_DISTANCE];

		final float mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);

		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;

		planes[planeOffset + PLANE_NORMAL_X] = a;
		planes[planeOffset + PLANE_NORMAL_Y] = b;
		planes[planeOffset + PLANE_NORMAL_Z] = c;
		planes[planeOffset + PLANE_DISTANCE] = d;

		planes[planeOffset + PLANE_EXTENT] = 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));
	}

	private void extractPlanes() {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) mvpMatrix;
		final float[] planes = this.planes;

		planes[PLANE_LEFT + PLANE_NORMAL_X] = matrix.a30() + matrix.a00();
		planes[PLANE_LEFT + PLANE_NORMAL_Y] = matrix.a31() + matrix.a01();
		planes[PLANE_LEFT + PLANE_NORMAL_Z] = matrix.a32() + matrix.a02();
		planes[PLANE_LEFT + PLANE_DISTANCE] = matrix.a33() + matrix.a03();

		planes[PLANE_RIGHT + PLANE_NORMAL_X] = matrix.a30() - matrix.a00();
		planes[PLANE_RIGHT + PLANE_NORMAL_Y] = matrix.a31() - matrix.a01();
		planes[PLANE_RIGHT + PLANE_NORMAL_Z] = matrix.a32() - matrix.a02();
		planes[PLANE_RIGHT + PLANE_DISTANCE] = matrix.a33() - matrix.a03();

		planes[PLANE_TOP + PLANE_NORMAL_X] = matrix.a30() - matrix.a10();
		planes[PLANE_TOP + PLANE_NORMAL_Y] = matrix.a31() - matrix.a11();
		planes[PLANE_TOP + PLANE_NORMAL_Z] = matrix.a32() - matrix.a12();
		planes[PLANE_TOP + PLANE_DISTANCE] = matrix.a33() - matrix.a13();

		planes[PLANE_BOTTOM + PLANE_NORMAL_X] = matrix.a30() + matrix.a10();
		planes[PLANE_BOTTOM + PLANE_NORMAL_Y] = matrix.a31() + matrix.a11();
		planes[PLANE_BOTTOM + PLANE_NORMAL_Z] = matrix.a32() + matrix.a12();
		planes[PLANE_BOTTOM + PLANE_DISTANCE] = matrix.a33() + matrix.a13();

		planes[PLANE_NEAR + PLANE_NORMAL_X] = matrix.a30() + matrix.a20();
		planes[PLANE_NEAR + PLANE_NORMAL_Y] = matrix.a31() + matrix.a21();
		planes[PLANE_NEAR + PLANE_NORMAL_Z] = matrix.a32() + matrix.a22();
		planes[PLANE_NEAR + PLANE_DISTANCE] = matrix.a33() + matrix.a23();

		//		planes[PLANE_FAR + PLANE_NORMAL_X] = matrix.a30() - matrix.a20();
		//		planes[PLANE_FAR + PLANE_NORMAL_Y] = matrix.a31() - matrix.a21();
		//		planes[PLANE_FAR + PLANE_NORMAL_Z] = matrix.a32() - matrix.a22();
		//		planes[PLANE_FAR + PLANE_DISTANCE] = matrix.a33() - matrix.a23();

		normalizePlane(PLANE_LEFT);
		normalizePlane(PLANE_RIGHT);
		normalizePlane(PLANE_TOP);
		normalizePlane(PLANE_BOTTOM);
		normalizePlane(PLANE_NEAR);
		//		normalizePlane(PLANE_FAR);
	}
}