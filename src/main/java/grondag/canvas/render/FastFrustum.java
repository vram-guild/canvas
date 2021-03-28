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

package grondag.canvas.render;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;

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

		float nx0 = corner.getX() / corner.getW();
		float ny0 = corner.getY() / corner.getW();
		float nz0 = corner.getZ() / corner.getW();

		corner.set(nx0, ny0, nz0, 1f);
		corner.transform(invViewMatrix);

		nx0 = corner.getX();
		ny0 = corner.getY();
		nz0 = corner.getZ();

		// near top right
		corner.set(1f, 1f, -1f, 1f);
		corner.transform(invProjMatrix);

		float nx1 = corner.getX() / corner.getW();
		float ny1 = corner.getY() / corner.getW();
		float nz1 = corner.getZ() / corner.getW();

		corner.set(nx1, ny1, nz1, 1f);
		corner.transform(invViewMatrix);

		nx1 = corner.getX();
		ny1 = corner.getY();
		nz1 = corner.getZ();

		// far lower left
		corner.set(-1f, -1f, 1f, 1f);
		corner.transform(invProjMatrix);

		float fx0 = corner.getX() / corner.getW();
		float fy0 = corner.getY() / corner.getW();
		float fz0 = corner.getZ() / corner.getW();

		corner.set(fx0, fy0, fz0, 1f);
		corner.transform(invViewMatrix);

		fx0 = corner.getX();
		fy0 = corner.getY();
		fz0 = corner.getZ();

		// far top right
		corner.set(1f, 1f, 1f, 1f);
		corner.transform(invProjMatrix);

		float fx1 = corner.getX() / corner.getW();
		float fy1 = corner.getY() / corner.getW();
		float fz1 = corner.getZ() / corner.getW();

		corner.set(fx1, fy1, fz1, 1f);
		corner.transform(invViewMatrix);

		fx1 = corner.getX();
		fy1 = corner.getY();
		fz1 = corner.getZ();

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
		final Vec3d vec = camera.getPos();
		lastViewX = vec.x;
		lastViewY = vec.y;
		lastViewZ = vec.z;

		modelMatrixExt.set(modelMatrix);
		projectionMatrixExt.set(projectionMatrix);

		mvpMatrixExt.loadIdentity();
		mvpMatrixExt.multiply(projectionMatrixExt);
		mvpMatrixExt.multiply(modelMatrixExt);

		// depends on mvpMatrix being complete
		extractPlanes();
	}
}
