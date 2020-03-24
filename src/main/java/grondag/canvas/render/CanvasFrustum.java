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

@Environment(EnvType.CLIENT)
public class CanvasFrustum extends Frustum {
	private final Matrix4f mvpMatrix = new Matrix4f();

	private Frustum testFrustum;

	private final Plane[] planes = {new Plane(), new Plane(), new Plane(), new Plane(), new Plane(), new Plane()};

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

		extractPlanes(planes, (Matrix4fExt)(Object) mvpMatrix);

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

		return planeAABBIntersect(cx, cy, cz, planes[0]) != OUTSIDE //  L
				&& planeAABBIntersect(cx, cy, cz, planes[1]) != OUTSIDE //  R
				&& planeAABBIntersect(cx, cy, cz, planes[2]) != OUTSIDE // TOP
				&& planeAABBIntersect(cx, cy, cz, planes[3]) != OUTSIDE // BOTT
				&& planeAABBIntersect(cx, cy, cz, planes[4]) != OUTSIDE // NEAR
				&& planeAABBIntersect(cx, cy, cz, planes[5]) != OUTSIDE; // FAR
	}

	static final int OUTSIDE = 0;
	static final int INSIDE = 1;
	static final int INTERSECTING = 2;

	private int planeAABBIntersect(float cx, float cy, float cz, Plane plane) {
		// c = center
		//		final float c = (b max + b min)/2;
		//		final float cx = (float) (box.x1 + 8 - cameraX);
		//		final float cy = (float) (box.y1 + 8 - cameraY);
		//		final float cz = (float) (box.z1 + 8 - cameraZ);

		// h = half diagonal
		//		float h = (final b max − final b min)/2;
		//		final float hx = (float) ((box.x2 - box.x1) / 2);
		//		final float hy = (float) ((box.y2 - box.y1) / 2);
		//		final float hz = (float) ((box.z2 - box.z1) / 2);

		// PERF: can hard-code the above is always the same

		//final float e = hx|nx| + hy|ny| + hz|nz|;
		final float e = plane.extent;
		//final float e = 8 * Math.abs(plane.a) + 8 * Math.abs(plane.b) + 8 * Math.abs(plane.c);

		final float s = cx * plane.a + cy * plane.b + cz * plane.c + plane.d;

		if(s - e > 0) {
			return OUTSIDE;
		} else if(s + e < 0) {
			return INSIDE;
		} else {
			return INTERSECTING;
		}
	}

	static class Plane {
		float a, b, c, d;
		float extent;
	}

	static void normalizePlane(Plane plane){
		final float mag = -(float) Math.sqrt(plane.a * plane.a + plane.b * plane.b + plane.c * plane.c);
		plane.a = plane.a / mag;
		plane.b = plane.b / mag;
		plane.c = plane.c / mag;
		plane.d = plane.d / mag;

		plane.extent = 8 * (Math.abs(plane.a) + Math.abs(plane.b) + Math.abs(plane.c));
	}

	static void extractPlanes(
			Plane[] planes,
			Matrix4fExt matrix)
	{
		// Left clipping plane
		planes[0].a = matrix.a30() + matrix.a00();
		planes[0].b = matrix.a31() + matrix.a01();
		planes[0].c = matrix.a32() + matrix.a02();
		planes[0].d = matrix.a33() + matrix.a03();

		// Right clipping plane
		planes[1].a = matrix.a30() - matrix.a00();
		planes[1].b = matrix.a31() - matrix.a01();
		planes[1].c = matrix.a32() - matrix.a02();
		planes[1].d = matrix.a33() - matrix.a03();

		// Top clipping plane
		planes[2].a = matrix.a30() - matrix.a10();
		planes[2].b = matrix.a31() - matrix.a11();
		planes[2].c = matrix.a32() - matrix.a12();
		planes[2].d = matrix.a33() - matrix.a13();

		// Bottom clipping plane
		planes[3].a = matrix.a30() + matrix.a10();
		planes[3].b = matrix.a31() + matrix.a11();
		planes[3].c = matrix.a32() + matrix.a12();
		planes[3].d = matrix.a33() + matrix.a13();

		// Near clipping plane
		planes[4].a = matrix.a30() + matrix.a20();
		planes[4].b = matrix.a31() + matrix.a21();
		planes[4].c = matrix.a32() + matrix.a22();
		planes[4].d = matrix.a33() + matrix.a23();

		// Far clipping plane
		planes[5].a = matrix.a30() - matrix.a20();
		planes[5].b = matrix.a31() - matrix.a21();
		planes[5].c = matrix.a32() - matrix.a22();
		planes[5].d = matrix.a33() - matrix.a23();

		normalizePlane(planes[0]);
		normalizePlane(planes[1]);
		normalizePlane(planes[2]);
		normalizePlane(planes[3]);
		normalizePlane(planes[4]);
		normalizePlane(planes[5]);
	}
}