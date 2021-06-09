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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;

import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.varia.CelestialObjectFunction.CelestialObjectOutput;

public final class ShadowMatrixData {
	private ShadowMatrixData() { }

	private static final Matrix4f shadowViewMatrix = new Matrix4f();
	private static final Matrix4fExt shadowViewMatrixExt = (Matrix4fExt) (Object) shadowViewMatrix;
	private static final Matrix4f shadowViewMatrixInv = new Matrix4f();
	private static final Matrix4fExt shadowViewMatrixInvExt = (Matrix4fExt) (Object) shadowViewMatrixInv;

	public static final int CASCADE_COUNT = 4;

	private static final Matrix4f[] shadowProjMatrix = new Matrix4f[CASCADE_COUNT];
	private static final Matrix4fExt[] shadowProjMatrixExt = new Matrix4fExt[CASCADE_COUNT];
	private static final Matrix4f[] shadowViewProjMatrix = new Matrix4f[CASCADE_COUNT];
	private static final Matrix4fExt[] shadowViewProjMatrixExt = new Matrix4fExt[CASCADE_COUNT];

	static {
		for (int i = 0; i < CASCADE_COUNT; ++i) {
			shadowProjMatrix[i] = new Matrix4f();
			shadowProjMatrixExt[i] = (Matrix4fExt) (Object) shadowProjMatrix[i];

			shadowViewProjMatrix[i] = new Matrix4f();
			shadowViewProjMatrixExt[i] = (Matrix4fExt) (Object) shadowViewProjMatrix[i];
		}
	}

	private static final Vector4f testVec = new Vector4f();
	private static float[] lastDx = new float[CASCADE_COUNT];
	private static float[] lastDy = new float[CASCADE_COUNT];
	private static double lastCameraX, lastCameraY, lastCameraZ;

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
		// To minimize the effect, we do two things:
		// 1) Keep pixel center near the camera.  This reduces the apparent warping
		// of pixel size on surfaces nearest the viewer.
		// 2) Clamp the angles of sun position to angles that result in an integer
		// number of pixels in the projected radius. Without this, there is constant
		// movement of pixel boundaries for shadows away from the camera.

		// WIP: correct these docs
		// Was previously using half the distance from the camera to the far plane, but that
		// isn't an accurate enough center position of a view frustum when the field of view is wide.
		// PERF: could be a little tighter by accounting for view distance - far corners aren't actually visible

		@SuppressWarnings("resource")
		final float viewDist = MinecraftClient.getInstance().gameRenderer.getViewDistance();

		// Half-way to view distance isn't the true center of the view frustum, but because
		// the far corners aren't actually visible it is close enough for now.
		final float halfDist = viewDist * 0.5f;

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
		shadowViewMatrix.loadIdentity();
		// FEAT: allow this to be configured by dimension - default value has north-south axis of rotation
		shadowViewMatrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90));
		shadowViewMatrix.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(skyOutput.zenithAngle));
		shadowViewMatrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(skyOutput.hourAngle));
		testVec.set(0, 1, 0, 0);
		testVec.transform(shadowViewMatrix);
		skyLightVector.set(testVec.getX(), testVec.getY(), testVec.getZ());

		// Use the unit vector we just computed to create a view matrix from perspective of the sky light.
		// Distance here isn't too picky, we need to ensure it is far enough away to contain any shadow-casting
		// geometry but not far enough to lose much precision in the depth (Z) dimension.
		shadowViewMatrixExt.lookAt(
			skyLightVector.getX() * radius, skyLightVector.getY() * radius, skyLightVector.getZ() * radius,
			0, 0, 0,
			0.0f, 0.0f, 1.0f);

		// Compute inverse while we're here
		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		shadowViewMatrixInv.invert();

		if (Pipeline.config().skyShadow != null) {
			// // Compute how much camera has moved in view x/y space.
			testVec.set((float) (cameraXd - lastCameraX), (float) (cameraYd - lastCameraY), (float) (cameraZd - lastCameraZ), 0.0f);
			testVec.transform(shadowViewMatrix);

			final float cdx = testVec.getX();
			final float cdy = testVec.getY();

			final int[] radii = Pipeline.config().skyShadow.cascadeRadii;

			updateCascadeInfo(0, radius, halfDist, radius, cdx, cdy);
			updateCascadeInfo(1, radii[0], radii[0], radius, cdx, cdy);
			updateCascadeInfo(2, radii[1], radii[1], radius, cdx, cdy);
			updateCascadeInfo(3, radii[2], radii[2], radius, cdx, cdy);
		}

		lastCameraX = cameraXd;
		lastCameraY = cameraYd;
		lastCameraZ = cameraZd;
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
		testVec.set(cameraVector.getX() * halfDist, cameraVector.getY() * halfDist, cameraVector.getZ() * halfDist, 1.0f);
		testVec.transform(shadowViewMatrix);

		float cx = testVec.getX();
		float cy = testVec.getY();
		float cz = testVec.getZ();

		cx = (float) (Math.floor(cx / worldPerPixel) * worldPerPixel) - dx;
		cy = (float) (Math.floor(cy / worldPerPixel) * worldPerPixel) - dy;
		cz = (float) (Math.ceil(cz / worldPerPixel) * worldPerPixel);

		// We previously use actual geometry depth to give better precision on Z.
		// However, scenes are so variable that this causes problems for optimizing polygonOffset
		// Z axis bounds are inverted because Z axis points towards negative end in OpenGL
		// This maps the near depth bound (center + radius) to -1 and the far depth bound (center - radius) to +1.

		// Construct ortho matrix using bounding sphere/box computed above.
		// Should give us a consistent size each frame, which helps prevent shimmering.
		shadowProjMatrixExt[cascade].setOrtho(
			cx - radius, cx + radius,
			cy - radius, cy + radius,
			-(cz + depthRadius), -(cz - depthRadius));

		final int offset = SHADOW_CENTER + cascade * 4;

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
		shadowViewMatrixExt.writeToBuffer(SHADOW_VIEW * 16, MATRIX_DATA);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		// reliable inversion of rotation matrix
		shadowViewMatrixInv.transpose();
		shadowViewMatrixInvExt.writeToBuffer(SHADOW_VIEW_INVERSE * 16, MATRIX_DATA);

		for (int i = 0; i < CASCADE_COUNT; ++i) {
			shadowProjMatrixExt[i].writeToBuffer((SHADOW_PROJ_0 + i) * 16, MATRIX_DATA);

			shadowViewProjMatrixExt[i].set(shadowProjMatrixExt[i]);
			shadowViewProjMatrixExt[i].multiply(shadowViewMatrixExt);
			shadowViewProjMatrixExt[i].writeToBuffer((SHADOW_VIEW_PROJ_0 + i) * 16, MATRIX_DATA);
		}
	}
}
