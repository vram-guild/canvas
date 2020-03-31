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
	private final Matrix4fExt lastProjectionMatrix = (Matrix4fExt)(Object) new Matrix4f();
	private final Matrix4fExt lastModelMatrix = (Matrix4fExt)(Object) new Matrix4f();
	private int version;

	private Frustum testFrustum;
	private float lastCameraX;
	private float lastCameraY;
	private float lastCameraZ;

	private float leftX, leftY, leftZ, leftDist;
	private float rightX, rightY, rightZ, rightDist;
	private float topX, topY, topZ, topDist;
	private float bottomX, bottomY, bottomZ, bottomDist;
	private float nearX, nearY, nearZ, nearDist;

	public CanvasFrustum() {
		super(dummyMatrix(), dummyMatrix());
	}

	private static Matrix4f dummyMatrix() {
		final Matrix4f dummt = new Matrix4f();
		dummt.loadIdentity();
		return dummt;
	}

	public int version() {
		return version;
	}

	public void prepare(Matrix4f modelMatrix, Matrix4f projectionMatrix, Camera camera) {
		final Vec3d vec = camera.getPos();
		final float cx = (float) vec.x;
		final float cy = (float) vec.y;
		final float cz = (float) vec.z;

		if(cx == lastCameraX && cy == lastCameraY && cx == lastCameraZ
				&& lastModelMatrix.matches(modelMatrix)
				&& lastProjectionMatrix.matches(projectionMatrix)) {
			return;
		}

		lastCameraX = cx;
		lastCameraY = cy;
		lastCameraZ = cz;
		lastModelMatrix.set(modelMatrix);
		lastProjectionMatrix.set(projectionMatrix);
		++version;

		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);

		extractPlanes();

		testFrustum = new Frustum(modelMatrix, projectionMatrix);

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

		if(cx * leftX + cy * leftY + cz * leftZ + leftDist > 0) {
			return false;
		}

		if(cx * rightX + cy * rightY + cz * rightZ + rightDist > 0) {
			return false;
		}

		if(cx * nearX + cy * nearY + cz * nearZ + nearDist > 0) {
			return false;
		}

		if(cx * topX + cy * topY + cz * topZ + topDist > 0) {
			return false;
		}

		if(cx * bottomX + cy * bottomY + cz * bottomZ + bottomDist > 0) {
			return false;
		}

		return true;
	}

	private void extractPlanes() {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) mvpMatrix;

		float a  = matrix.a30() + matrix.a00();
		float b  = matrix.a31() + matrix.a01();
		float c  = matrix.a32() + matrix.a02();
		float d  = matrix.a33() + matrix.a03();
		float mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);
		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;
		leftX = a;
		leftY = b;
		leftZ = c;
		// subtract maximum extent of box center to corner
		leftDist = d - 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));

		a  = matrix.a30() - matrix.a00();
		b  = matrix.a31() - matrix.a01();
		c  = matrix.a32() - matrix.a02();
		d  = matrix.a33() - matrix.a03();
		mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);
		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;
		rightX = a;
		rightY = b;
		rightZ = c;
		rightDist = d - 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));

		a  = matrix.a30() - matrix.a10();
		b  = matrix.a31() - matrix.a11();
		c  = matrix.a32() - matrix.a12();
		d  = matrix.a33() - matrix.a13();
		mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);
		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;
		topX = a;
		topY = b;
		topZ = c;
		topDist = d - 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));

		a  = matrix.a30() + matrix.a10();
		b  = matrix.a31() + matrix.a11();
		c  = matrix.a32() + matrix.a12();
		d  = matrix.a33() + matrix.a13();
		mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);
		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;
		bottomX = a;
		bottomY = b;
		bottomZ = c;
		bottomDist = d - 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));

		a  = matrix.a30() + matrix.a20();
		b  = matrix.a31() + matrix.a21();
		c  = matrix.a32() + matrix.a22();
		d  = matrix.a33() + matrix.a23();
		mag = -1f / (float) Math.sqrt(a * a + b * b + c * c);
		a *= mag;
		b *= mag;
		c *= mag;
		d *= mag;
		nearX = a;
		nearY = b;
		nearZ = c;
		nearDist = d - 8 * (Math.abs(a) + Math.abs(b) + Math.abs(c));
	}
}