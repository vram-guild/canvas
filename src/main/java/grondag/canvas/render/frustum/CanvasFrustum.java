/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.render.frustum;

import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.mixinterface.Matrix4fExt;

/**
 * Plane equation derivations based on:
 * "Fast Extraction of Viewing Frustum Planes from the World- View-Projection Matrix"
 * Gill Gribb, Klaus Hartmann
 * https://www.gamedevs.org/uploads/fast-extraction-viewing-frustum-planes-from-world-view-projection-matrix.pdf
 *
 * <p>AABB test method based on work by Ville Miettinen
 * as described in Real-Time Rendering, Fourth Edition (Page 971). CRC Press.
 * Abbey, Duane C.; Haines, Eric; Hoffman, Naty.
 */
@Environment(EnvType.CLIENT)
public abstract class CanvasFrustum extends Frustum {
	protected static final float MIN_GAP = 0.0001f;
	protected final Matrix4f mvpMatrix = new Matrix4f();
	protected final Matrix4fExt mvpMatrixExt = (Matrix4fExt) (Object) mvpMatrix;
	protected final Matrix4f projectionMatrix = new Matrix4f();
	protected final Matrix4fExt projectionMatrixExt = (Matrix4fExt) (Object) projectionMatrix;
	protected final Matrix4f modelMatrix = new Matrix4f();
	protected final Matrix4fExt modelMatrixExt = (Matrix4fExt) (Object) modelMatrix;

	protected double lastCameraX = Double.MAX_VALUE;
	protected double lastCameraY = Double.MAX_VALUE;
	protected double lastCameraZ = Double.MAX_VALUE;

	// NB: distance (w) and subtraction are baked into region extents but must be done for other box tests
	protected float leftX, leftY, leftZ, leftW, leftXe, leftYe, leftZe, leftRegionExtent;
	protected float rightX, rightY, rightZ, rightW, rightXe, rightYe, rightZe, rightRegionExtent;
	protected float topX, topY, topZ, topW, topXe, topYe, topZe, topRegionExtent;
	protected float bottomX, bottomY, bottomZ, bottomW, bottomXe, bottomYe, bottomZe, bottomRegionExtent;
	protected float nearX, nearY, nearZ, nearW, nearXe, nearYe, nearZe, nearRegionExtent;

	public CanvasFrustum() {
		super(dummyMatrix(), dummyMatrix());
	}

	protected static Matrix4f dummyMatrix() {
		final Matrix4f dummy = new Matrix4f();
		dummy.setIdentity();
		return dummy;
	}

	public final Matrix4fExt projectionMatrix() {
		return projectionMatrixExt;
	}

	public final Matrix4fExt modelMatrix() {
		return modelMatrixExt;
	}

	@Override
	public final boolean isVisible(AABB box) {
		return cubeInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public boolean cubeInFrustum(double x0, double y0, double z0, double x1, double y1, double z1) {
		final double hdx = 0.5 * (x1 - x0);
		final double hdy = 0.5 * (y1 - y0);
		final double hdz = 0.5 * (z1 - z0);

		assert hdx > 0;
		assert hdy > 0;
		assert hdz > 0;

		final float cx = (float) (x0 + hdx - lastCameraX);
		final float cy = (float) (y0 + hdy - lastCameraY);
		final float cz = (float) (z0 + hdz - lastCameraZ);

		if (cx * leftX + cy * leftY + cz * leftZ + leftW - (hdx * leftXe + hdy * leftYe + hdz * leftZe) > 0) {
			return false;
		}

		if (cx * rightX + cy * rightY + cz * rightZ + rightW - (hdx * rightXe + hdy * rightYe + hdz * rightZe) > 0) {
			return false;
		}

		if (cx * nearX + cy * nearY + cz * nearZ + nearW - (hdx * nearXe + hdy * nearYe + hdz * nearZe) > 0) {
			return false;
		}

		if (cx * topX + cy * topY + cz * topZ + topW - (hdx * topXe + hdy * topYe + hdz * topZe) > 0) {
			return false;
		}

		return !(cx * bottomX + cy * bottomY + cz * bottomZ + bottomW - (hdx * bottomXe + hdy * bottomYe + hdz * bottomZe) > 0);
	}

	protected final void extractPlanes() {
		final Matrix4fExt matrix = mvpMatrixExt;
		final float a00 = matrix.a00();
		final float a01 = matrix.a01();
		final float a02 = matrix.a02();
		final float a03 = matrix.a03();
		final float a10 = matrix.a10();
		final float a11 = matrix.a11();
		final float a12 = matrix.a12();
		final float a13 = matrix.a13();
		final float a30 = matrix.a30();
		final float a31 = matrix.a31();
		final float a32 = matrix.a32();
		final float a33 = matrix.a33();

		float x = a30 + a00;
		float y = a31 + a01;
		float z = a32 + a02;
		float w = a33 + a03;
		float mag = -Mth.fastInvSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		leftX = x;
		leftY = y;
		leftZ = z;
		leftW = w;
		float xe = Math.abs(x);
		float ye = Math.abs(y);
		float ze = Math.abs(z);
		leftXe = xe;
		leftYe = ye;
		leftZe = ze;
		leftRegionExtent = w - 8 * (xe + ye + ze) - MIN_GAP;

		x = a30 - a00;
		y = a31 - a01;
		z = a32 - a02;
		w = a33 - a03;
		mag = -Mth.fastInvSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		rightX = x;
		rightY = y;
		rightZ = z;
		rightW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		rightXe = xe;
		rightYe = ye;
		rightZe = ze;
		rightRegionExtent = w - 8 * (xe + ye + ze) - MIN_GAP;

		x = a30 - a10;
		y = a31 - a11;
		z = a32 - a12;
		w = a33 - a13;
		mag = -Mth.fastInvSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		topX = x;
		topY = y;
		topZ = z;
		topW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		topXe = xe;
		topYe = ye;
		topZe = ze;
		topRegionExtent = w - 8 * (xe + ye + ze) - MIN_GAP;

		x = a30 + a10;
		y = a31 + a11;
		z = a32 + a12;
		w = a33 + a13;
		mag = -Mth.fastInvSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		bottomX = x;
		bottomY = y;
		bottomZ = z;
		bottomW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		bottomXe = xe;
		bottomYe = ye;
		bottomZe = ze;
		bottomRegionExtent = w - 8 * (xe + ye + ze) - MIN_GAP;

		x = a30 + matrix.a20();
		y = a31 + matrix.a21();
		z = a32 + matrix.a22();
		w = a33 + matrix.a23();
		mag = -Mth.fastInvSqrt(x * x + y * y + z * z);
		x *= mag;
		y *= mag;
		z *= mag;
		w *= mag;
		nearX = x;
		nearY = y;
		nearZ = z;
		nearW = w;
		xe = Math.abs(x);
		ye = Math.abs(y);
		ze = Math.abs(z);
		nearXe = xe;
		nearYe = ye;
		nearZe = ze;
		nearRegionExtent = w - 8 * (xe + ye + ze) - MIN_GAP;
	}
}
