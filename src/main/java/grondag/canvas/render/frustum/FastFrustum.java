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
import com.mojang.math.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.CanvasMath;

@Environment(EnvType.CLIENT)
public class FastFrustum extends CanvasFrustum {
	protected float circumCenterX, circumCenterY, circumCenterZ, circumRadius;

	public float circumCenterX() {
		return circumCenterX;
	}

	public float circumCenterY() {
		return circumCenterY;
	}

	public float circumCenterZ() {
		return circumCenterZ;
	}

	public float circumRadius() {
		return circumRadius;
	}

	public void computeCircumCenter(Matrix4f invViewMatrix, Matrix4f invProjMatrix) {
		final Vector4f corner = new Vector4f();

		// near lower left
		corner.set(-1f, -1f, -1f, 1f);
		corner.transform(invProjMatrix);

		float nx0 = corner.x() / corner.w();
		float ny0 = corner.y() / corner.w();
		float nz0 = corner.z() / corner.w();

		corner.set(nx0, ny0, nz0, 1f);
		corner.transform(invViewMatrix);

		nx0 = corner.x();
		ny0 = corner.y();
		nz0 = corner.z();

		// near top right
		corner.set(1f, 1f, -1f, 1f);
		corner.transform(invProjMatrix);

		float nx1 = corner.x() / corner.w();
		float ny1 = corner.y() / corner.w();
		float nz1 = corner.z() / corner.w();

		corner.set(nx1, ny1, nz1, 1f);
		corner.transform(invViewMatrix);

		nx1 = corner.x();
		ny1 = corner.y();
		nz1 = corner.z();

		// far lower left
		corner.set(-1f, -1f, 1f, 1f);
		corner.transform(invProjMatrix);

		float fx0 = corner.x() / corner.w();
		float fy0 = corner.y() / corner.w();
		float fz0 = corner.z() / corner.w();

		corner.set(fx0, fy0, fz0, 1f);
		corner.transform(invViewMatrix);

		fx0 = corner.x();
		fy0 = corner.y();
		fz0 = corner.z();

		// far top right
		corner.set(1f, 1f, 1f, 1f);
		corner.transform(invProjMatrix);

		float fx1 = corner.x() / corner.w();
		float fy1 = corner.y() / corner.w();
		float fz1 = corner.z() / corner.w();

		corner.set(fx1, fy1, fz1, 1f);
		corner.transform(invViewMatrix);

		fx1 = corner.x();
		fy1 = corner.y();
		fz1 = corner.z();

		final float a = CanvasMath.dist(fx0, fy0, fz0, fx1, fy1, fz1);
		final float b = CanvasMath.dist(nx0, ny0, nz0, nx1, ny1, nz1);
		final float c = CanvasMath.dist(nx0, ny0, nz0, fx0, fy0, fz0);
		final float ab = a - b;

		circumRadius = c * (float) Math.sqrt((a * b + c * c) / (4 * c * c - ab * ab));

		// find center depth from near to far plane
		// is height of isoceles triangle formed by near plane points as base and
		// circumradias as sides
		final float centerDepth = 0.5f * (float) Math.sqrt(4 * circumRadius * circumRadius + b * b);

		// near plane center
		final float ncx = (nx0 + nx1) * 0.5f;
		final float ncy = (ny0 + ny1) * 0.5f;
		final float ncz = (nz0 + nz1) * 0.5f;

		// far plane center
		final float fcx = (fx0 + fx1) * 0.5f;
		final float fcy = (fy0 + fy1) * 0.5f;
		final float fcz = (fz0 + fz1) * 0.5f;

		final float depth = CanvasMath.dist(ncx, ncy, ncz, fcx, fcy, fcz);

		final float centerFactor = centerDepth / depth;

		// interpolate to get circumcenter
		circumCenterX = ncx + centerFactor * (fcx - ncx);
		circumCenterY = ncy + centerFactor * (fcy - ncy);
		circumCenterZ = ncz + centerFactor * (fcz - ncz);
	}

	public void prepare(Matrix4f modelMatrix, float tickDelta, Camera camera, Matrix4f projectionMatrix) {
		final Vec3 vec = camera.getPosition();
		lastCameraX = vec.x;
		lastCameraY = vec.y;
		lastCameraZ = vec.z;

		modelMatrixExt.set(modelMatrix);
		projectionMatrixExt.set(projectionMatrix);

		mvpMatrixExt.loadIdentity();
		mvpMatrixExt.multiply(projectionMatrixExt);
		mvpMatrixExt.multiply(modelMatrixExt);

		// depends on mvpMatrix being complete
		extractPlanes();
	}
}
