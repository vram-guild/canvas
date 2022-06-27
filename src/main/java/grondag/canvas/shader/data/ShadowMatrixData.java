/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.shader.data;

import static grondag.canvas.shader.data.FloatData.SHADOW_CENTER;
import static grondag.canvas.shader.data.MatrixData.MATRIX_DATA;
import static grondag.canvas.shader.data.MatrixData.SHADOW_PROJ_0;
import static grondag.canvas.shader.data.MatrixData.SHADOW_VIEW;
import static grondag.canvas.shader.data.MatrixData.SHADOW_VIEW_INVERSE;
import static grondag.canvas.shader.data.MatrixData.SHADOW_VIEW_PROJ_0;
import static grondag.canvas.shader.data.ShaderDataManager.cameraVector;
import static grondag.canvas.shader.data.ShaderDataManager.cameraXd;
import static grondag.canvas.shader.data.ShaderDataManager.cameraYd;
import static grondag.canvas.shader.data.ShaderDataManager.cameraZd;
import static grondag.canvas.shader.data.ShaderDataManager.skyLightVector;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import io.vram.frex.api.math.FastMatrix4f;

import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.varia.CelestialObjectFunction.CelestialObjectOutput;

public final class ShadowMatrixData {
	private ShadowMatrixData() { }

	public static final Matrix4f shadowViewMatrix = new Matrix4f();
	private static final FastMatrix4f shadowViewMatrixExt = (FastMatrix4f) (Object) shadowViewMatrix;
	private static final Matrix4f shadowViewMatrixInv = new Matrix4f();
	private static final FastMatrix4f shadowViewMatrixInvExt = (FastMatrix4f) (Object) shadowViewMatrixInv;

	public static final int CASCADE_COUNT = 4;

	private static final Matrix4f[] shadowProjMatrix = new Matrix4f[CASCADE_COUNT];
	private static final FastMatrix4f[] shadowProjMatrixExt = new FastMatrix4f[CASCADE_COUNT];
	private static final Matrix4f[] shadowViewProjMatrix = new Matrix4f[CASCADE_COUNT];
	private static final FastMatrix4f[] shadowViewProjMatrixExt = new FastMatrix4f[CASCADE_COUNT];

	public static final float[] cascadeCentersAndRadii = new float[16];

	public static Matrix4f maxCascadeProjMatrix() {
		return shadowProjMatrix[0];
	}

	static {
		for (int i = 0; i < CASCADE_COUNT; ++i) {
			shadowProjMatrix[i] = new Matrix4f();
			shadowProjMatrixExt[i] = (FastMatrix4f) (Object) shadowProjMatrix[i];

			shadowViewProjMatrix[i] = new Matrix4f();
			shadowViewProjMatrixExt[i] = (FastMatrix4f) (Object) shadowViewProjMatrix[i];
		}
	}

	private static final Vector4f testVec = new Vector4f();
	private static float[] lastDx = new float[CASCADE_COUNT];
	private static float[] lastDy = new float[CASCADE_COUNT];
	private static double lastCameraX, lastCameraY, lastCameraZ;

	private static float regionMaxExtent;

	private static void computeShadowMatrices(Camera camera, float tickDelta, CelestialObjectOutput skyOutput) {
		// We need to keep the skylight projection consistently aligned to
		// pixels in the shadowmap texture.  The alignment must be to world
		// coordinates in the x/y axis of the skylight projection.
		// Both the frustum center and relative light position move as the camera moves,
		// which causes shimmering if we don't adjust for this movement.

		// Because all of our coordinates and matrices at this point are relative to camera,
		// we can't use them directly to test for alignment to world coordinates.
		// To correct for this, each frame we compute the X/Y movement of the
		// camera in projected light space and adjust an offset that snaps the camera
		// to a nearby pixel boundary.

		// As the sun moves, the shape and size of individual pixels changes.  This
		// can also cause aliasing and shimmering and cannot be fully countered.
		// To minimize the effect, we keep pixel center near the camera.
		// This reduces the apparent warping of pixel size on surfaces nearest the viewer.

		@SuppressWarnings("resource")
		final float viewDist = Math.min(Configurator.shadowMaxDistance * 16, Minecraft.getInstance().gameRenderer.getRenderDistance());
		// Half-way to view distance isn't the true center of the view frustum, but because
		// the far corners aren't actually visible it is close enough for now.
		// EXPERIMENTAL: allow adjusting magnitude of the effective shadow center
		final float halfDist = Configurator.shadowCenterFactor * viewDist * 0.5f;

		// Bounding sphere/box distance for the largest cascade.  Relies on assumption the frustum
		// will be wider than it is long, and if not then view distance should be adequate.
		// We find the point that is halfDist deep and at our view distance.  Note this is a right
		// triangle with view distance as hypotenuse and half dist as on of the legs.
		//
		//		    ------ far plane (view distance)
		//             |
		//             |
		//  half vd    *---- r
		//             |  /
		//             | /
		//  camera     c
		//

		final int radius = (int) Math.ceil(Math.sqrt(viewDist * viewDist - halfDist * halfDist));

		// Compute sky light vector transform - points towards the sun
		shadowViewMatrix.setIdentity();
		// FEAT: allow this to be configured by dimension - default value has north-south axis of rotation
		shadowViewMatrix.multiply(Vector3f.YP.rotationDegrees(-90));
		shadowViewMatrix.multiply(Vector3f.ZP.rotationDegrees(skyOutput.zenithAngle));
		shadowViewMatrix.multiply(Vector3f.XP.rotationDegrees(skyOutput.hourAngle));
		testVec.set(0, 1, 0, 0);
		testVec.transform(shadowViewMatrix);
		skyLightVector.set(testVec.x(), testVec.y(), testVec.z());

		// Use the unit vector we just computed to create a view matrix from perspective of the sky light.
		// Distance here isn't too picky, we need to ensure it is far enough away to contain any shadow-casting
		// geometry but not far enough to lose much precision in the depth (Z) dimension.
		shadowViewMatrixExt.f_setLookAt(
			skyLightVector.x() * radius, skyLightVector.y() * radius, skyLightVector.z() * radius,
			0, 0, 0,
			0.0f, 0.0f, 1.0f);

		// Compute inverse while we're here
		shadowViewMatrixInvExt.f_set(shadowViewMatrixExt);
		shadowViewMatrixInv.invert();

		if (Pipeline.config().skyShadow != null) {
			// Compute how much camera has moved in view x/y space.
			testVec.set((float) (cameraXd - lastCameraX), (float) (cameraYd - lastCameraY), (float) (cameraZd - lastCameraZ), 0.0f);
			testVec.transform(shadowViewMatrix);

			final float cdx = testVec.x();
			final float cdy = testVec.y();

			final int[] radii = Pipeline.config().skyShadow.cascadeRadii;

			updateCascadeInfo(0, radius, halfDist, radius, cdx, cdy);
			updateCascadeInfo(1, radii[0], radii[0] * Configurator.shadowCenterFactor, radius, cdx, cdy);
			updateCascadeInfo(2, radii[1], radii[1] * Configurator.shadowCenterFactor, radius, cdx, cdy);
			updateCascadeInfo(3, radii[2], radii[2] * Configurator.shadowCenterFactor, radius, cdx, cdy);
		}

		lastCameraX = cameraXd;
		lastCameraY = cameraYd;
		lastCameraZ = cameraZd;

		testVec.set(8f, -8f, -8f, 0);
		testVec.transform(shadowViewMatrix);
		float rme = testVec.x() * testVec.x() + testVec.y() * testVec.y() + testVec.z() * testVec.z();

		testVec.set(8f, -8f, 8f, 0);
		testVec.transform(shadowViewMatrix);
		rme = Math.max(rme, testVec.x() * testVec.x() + testVec.y() * testVec.y() + testVec.z() * testVec.z());

		testVec.set(8f, 8f, -8f, 0);
		testVec.transform(shadowViewMatrix);
		rme = Math.max(rme, testVec.x() * testVec.x() + testVec.y() * testVec.y() + testVec.z() * testVec.z());

		testVec.set(8f, 8f, 8f, 0);
		testVec.transform(shadowViewMatrix);
		rme = Math.max(rme, testVec.x() * testVec.x() + testVec.y() * testVec.y());

		regionMaxExtent = (float) Math.sqrt(rme);
	}

	/**
	 *
	 * @param cascade  cascade index, 0 is largest (least detail) and 3 is smalled (most detail)
	 * @param radius   radius of bounding box / sphere - same as half distance for all but largest
	 * @param halfDist distance from camera to center of of bounding box / sphere - same as radius for all but largest
	 * @param depthRadius depth radius to use for depth projection - must always encompass entire scene depth
	 * @param cdx	   movement of camera on X axis of light view since last frame
	 * @param cdy	   movement of camera on Y axis of light view since last frame
	 */
	private static void updateCascadeInfo(int cascade, int radius, float halfDist, int depthRadius, float cdx, float cdy) {
		// Accumulate camera adjustment
		float dx = lastDx[cascade] + cdx;
		float dy = lastDy[cascade] + cdy;

		// Clamp accumulated camera adjustment to pixel boundary.
		// This keep the projection center near the camera position.
		final double worldPerPixel = 2.0 * radius / Pipeline.skyShadowSize;
		dx = (float) (dx - Math.floor(dx / worldPerPixel) * worldPerPixel);
		dy = (float) (dy - Math.floor(dy / worldPerPixel) * worldPerPixel);

		// NaN values sometime crop up in edge cases, esp during initialization.
		// If we don't correct for them then the accumulated adjustment becomes Nan
		// and never recovers.
		if (Float.isNaN(dx)) {
			dx = 0f;
		}

		if (Float.isNaN(dy)) {
			dy = 0f;
		}

		// Find the center of our projection.
		testVec.set(cameraVector.x() * halfDist, cameraVector.y() * halfDist, cameraVector.z() * halfDist, 1.0f);
		testVec.transform(shadowViewMatrix);

		float cx = testVec.x();
		float cy = testVec.y();
		float cz = testVec.z();

		cx = (float) (Math.floor(cx / worldPerPixel) * worldPerPixel) - dx;
		cy = (float) (Math.floor(cy / worldPerPixel) * worldPerPixel) - dy;
		cz = (float) (Math.ceil(cz / worldPerPixel) * worldPerPixel);

		// We previously use actual geometry depth to give better precision on Z.
		// However, scenes are so variable that this causes problems for optimizing polygonOffset
		// Z axis bounds are inverted because Z axis points towards negative end in OpenGL
		// This maps the near depth bound (center + radius) to -1 and the far depth bound (center - radius) to +1.

		// Construct ortho matrix using bounding sphere/box computed above.
		// Should give us a consistent size each frame, which helps prevent shimmering.
		shadowProjMatrixExt[cascade].f_setOrtho(
			cx - radius, cx + radius,
			cy - radius, cy + radius,
			-(cz + depthRadius), -(cz - depthRadius));

		final int localOffset = cascade * 4;
		final int offset = SHADOW_CENTER + localOffset;

		cascadeCentersAndRadii[localOffset] = cx;
		cascadeCentersAndRadii[localOffset + 1] = cy;
		cascadeCentersAndRadii[localOffset + 2] = cz;
		cascadeCentersAndRadii[localOffset + 3] = radius;

		FloatData.FLOAT_VECTOR_DATA.put(offset, cx);
		FloatData.FLOAT_VECTOR_DATA.put(offset + 1, cy);
		FloatData.FLOAT_VECTOR_DATA.put(offset + 2, cz);
		FloatData.FLOAT_VECTOR_DATA.put(offset + 3, radius);

		lastDx[cascade] = dx;
		lastDy[cascade] = dy;
	}

	static void update(Camera camera, float tickDelta, CelestialObjectOutput skyoutput) {
		computeShadowMatrices(camera, tickDelta, skyoutput);

		// shadow perspective were computed earlier
		shadowViewMatrixExt.f_writeToBuffer(SHADOW_VIEW * 16, MATRIX_DATA);

		shadowViewMatrixInvExt.f_set(shadowViewMatrixExt);
		// reliable inversion of rotation matrix
		shadowViewMatrixInv.transpose();
		shadowViewMatrixInvExt.f_writeToBuffer(SHADOW_VIEW_INVERSE * 16, MATRIX_DATA);

		for (int i = 0; i < CASCADE_COUNT; ++i) {
			shadowProjMatrixExt[i].f_writeToBuffer((SHADOW_PROJ_0 + i) * 16, MATRIX_DATA);

			shadowViewProjMatrixExt[i].f_set(shadowProjMatrixExt[i]);
			shadowViewProjMatrixExt[i].f_mul(shadowViewMatrixExt);
			shadowViewProjMatrixExt[i].f_writeToBuffer((SHADOW_VIEW_PROJ_0 + i) * 16, MATRIX_DATA);
		}
	}

	public static float regionMaxExtent() {
		return regionMaxExtent;
	}
}
