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

package grondag.canvas.varia;

import static grondag.canvas.varia.WorldDataManager.cameraXd;
import static grondag.canvas.varia.WorldDataManager.cameraYd;
import static grondag.canvas.varia.WorldDataManager.cameraZd;
import static grondag.canvas.varia.WorldDataManager.lastSkyLightPosition;
import static grondag.canvas.varia.WorldDataManager.skyLightPosition;
import static grondag.canvas.varia.WorldDataManager.skyLightVector;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.FastFrustum;
import grondag.canvas.terrain.occlusion.geometry.TerrainBounds;
import grondag.canvas.varia.CelestialObjectFunction.CelestialObjectOutput;

/**
 * Describes how vertex coordinates relate to world and camera geometry.
 * Currently vertex collectors don't mix so not part of render state
 * but kept as a global indicator to allow for checking and in-shader information.
 *
 * <p>Except as noted below, GL state is always assumed to have the projection
 * matrix set and view matrix set to identity. This is the default matrix
 * state during work render.
 */
public enum MatrixState {
	/**
	 * Vertex coordinates in frx_startVertex are relative to the camera position.
	 * Coordinates and normals are unrotated.
	 * frx_modelOriginWorldPos() returns camera position.
	 */
	CAMERA,

	/**
	 * Vertex coordinates in frx_startVertex are relative to the origin of a
	 * "cluster" of world render regions.
	 * Coordinates and normals are unrotated.
	 * frx_modelOriginWorldPos() returns the cluster origin.
	 */
	REGION,

	/**
	 * Vertex coordinates are relative to the screen.  No transforms should be applied.
	 * Intended for Hand//GUI rendering.
	 */
	SCREEN;

	private static MatrixState current = CAMERA;

	private static final Matrix3f IDENTITY = new Matrix3f();

	static {
		IDENTITY.loadIdentity();
	}

	public static MatrixState getModelOrigin() {
		return current;
	}

	public static void set(MatrixState val) {
		assert val != null;
		current = val;
	}

	private static final Vector4f testVec = new Vector4f();
	private static float lastDx, lastDy;
	private static double lastCameraX, lastCameraY, lastCameraZ;

	private static float clampAngle(float angle, int radius) {
		if (radius == 0) {
			return angle;
		}

		final double sin = Math.sin(Math.toRadians(angle));
		final double cos = Math.cos(Math.toRadians(angle));

		final double csin = Math.round(sin * radius) / (double) radius;
		final double ccos = Math.round(cos * radius) / (double) radius;

		return (float) Math.toDegrees(Math.atan2(csin, ccos));
	}

	private static void computeShadowMatrices(Camera camera, float tickDelta, TerrainBounds bounds, CelestialObjectOutput skyOutput) {
		// We need to keep the skylight projection consistently aligned to
		// pixels in the shadowmap texture.  The alignment must be to world
		// coordinates in the x/y axis of the skylight perspective.
		// Both the frustum center and light position move as the camera moves,
		// which causes shimmering if we don't adjust for this movement.

		// Because all of our coordinates and matrices at this point are relative to camera,
		// we can't test use them for the alignment to world coordinates.
		// So we compute the position of the frustum center in world space in a
		// projection centered on world origin. Depth doesn't matter here.
		// As the camera moves, the x/y distance of its position from the origin
		// indicate how much we need to translate the shadowmap projection to maintain alignment.

		// Frustum center is at most half view distance away from each frustum plane
		// Expanding in each direction by that much should enclose the visible scene
		// (Approximate because view frustum isn't a simple box.)
		// Note the Y-axis pffset is inverted because MC Y is inverted relative to OpenGL/matrix transform

		// To avoid precision issues at the edge of the world, use a world boundary
		// that is relatively close - keeping them at regular intervals.

		// PERF: could be a little tighter by accounting for view distance - far corners aren't actually visible
		final int radius = Math.round(cleanFrustum.circumRadius());

		shadowViewMatrix.loadIdentity();
		// FEAT: allow this to be configured by dimension - default value has north-south axis of rotation
		shadowViewMatrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90));
		shadowViewMatrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(clampAngle(skyOutput.zenithAngle, Pipeline.skyShadowSize / 2)));
		shadowViewMatrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(clampAngle(skyOutput.hourAngle, Pipeline.skyShadowSize / 2)));

		testVec.set(0, 1, 0, 0);
		testVec.transform(shadowViewMatrix);
		skyLightVector.set(testVec.getX(), testVec.getY(), testVec.getZ());

		lastSkyLightPosition.set(skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ());
		skyLightPosition.set(skyLightVector.getX() * 2048, skyLightVector.getY() * 2048, skyLightVector.getZ() * 2048);

		shadowViewMatrixExt.lookAt(
			skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ(),
			0, 0, 0,
			0.0f, 0.0f, 1.0f);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		shadowViewMatrixInv.invert();

		testVec.set((float) (cameraXd - lastCameraX), (float) (cameraYd - lastCameraY), (float) (cameraZd - lastCameraZ), 0.0f);
		testVec.transform(shadowViewMatrix);

		float dx = lastDx + testVec.getX();
		float dy = lastDy + testVec.getY();

		// clamp to pixel boundary
		final double worldPerPixel = 2.0 * radius / Pipeline.skyShadowSize;
		dx = (float) (dx - Math.floor(dx / worldPerPixel) * worldPerPixel);
		dy = (float) (dy - Math.floor(dy / worldPerPixel) * worldPerPixel);

		if (Float.isNaN(dx)) {
			dx = 0f;
		}

		if (Float.isNaN(dy)) {
			dy = 0f;
		}

		bounds.computeViewBounds(shadowViewMatrixExt, WorldDataManager.cameraX, WorldDataManager.cameraY, WorldDataManager.cameraZ);

		float cx = (bounds.minViewX() + bounds.maxViewX()) * 0.5f;
		float cy = (bounds.minViewY() + bounds.maxViewY()) * 0.5f;

		cx = (float) (Math.floor(cx / worldPerPixel) * worldPerPixel) - dx;
		cy = (float) (Math.floor(cy / worldPerPixel) * worldPerPixel) - dy;

		// We use actual geometry depth to give better precision on Z.
		// but clamp to pixel boundaries to minimize aliasing.
		// Z axis inverted to match depth axis in OpenGL
		final float maxZ = -(float) (Math.ceil(bounds.minViewZ() / worldPerPixel) * worldPerPixel);
		final float minZ = -(float) (Math.ceil(bounds.maxViewZ() / worldPerPixel) * worldPerPixel);

		// Construct ortho matrix using bounding sphere box computed above.
		// Should give us a consistent size each frame until the sun moves.
		shadowProjMatrixExt.setOrtho(
			cx - radius, cx + radius,
			cy - radius, cy + radius,
			minZ, maxZ);

		shadowDepth = Math.abs(bounds.maxViewZ() - bounds.minViewZ());

		lastDx = dx;
		lastDy = dy;
		lastCameraX = cameraXd;
		lastCameraY = cameraYd;
		lastCameraZ = cameraZd;
	}

	static void update(MatrixStack.Entry view, Matrix4f projectionMatrix, Camera camera, float tickDelta, TerrainBounds bounds) {
		// write values for prior frame before updating
		viewMatrixExt.writeToBuffer(VIEW_LAST * 16, DATA);
		projMatrixExt.writeToBuffer(PROJ_LAST * 16, DATA);
		viewProjMatrixExt.writeToBuffer(VP_LAST * 16, DATA);
		cleanProjMatrixExt.writeToBuffer(CLEAN_PROJ_LAST * 16, DATA);
		cleanViewProjMatrixExt.writeToBuffer(CLEAN_VP_LAST * 16, DATA);

		((Matrix3fExt) (Object) viewNormalMatrix).set((Matrix3fExt) (Object) view.getNormal());

		viewMatrixExt.set((Matrix4fExt) (Object) view.getModel());
		viewMatrixExt.writeToBuffer(VIEW * 16, DATA);
		projMatrixExt.set((Matrix4fExt) (Object) projectionMatrix);
		projMatrixExt.writeToBuffer(PROJ * 16, DATA);

		viewMatrixInvExt.set(viewMatrixExt);
		// reliable inversion of rotation matrix
		viewMatrixInv.transpose();
		viewMatrixInvExt.writeToBuffer(VIEW_INVERSE * 16, DATA);

		projMatrixInvExt.set(projMatrixExt);
		projMatrixInv.invert();
		projMatrixInvExt.writeToBuffer(PROJ_INVERSE * 16, DATA);

		viewProjMatrixExt.set(projMatrixExt);
		viewProjMatrixExt.multiply(viewMatrixExt);
		viewProjMatrixExt.writeToBuffer(VP * 16, DATA);

		viewProjMatrixInvExt.set(viewMatrixInvExt);
		viewProjMatrixInvExt.multiply(projMatrixInvExt);
		viewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, DATA);

		computeCleanProjection(camera, tickDelta);
		cleanProjMatrixExt.writeToBuffer(CLEAN_PROJ * 16, DATA);
		cleanProjMatrixInvExt.writeToBuffer(CLEAN_PROJ_INVERSE * 16, DATA);

		cleanViewProjMatrixExt.set(cleanProjMatrixExt);
		cleanViewProjMatrixExt.multiply(viewMatrixExt);
		cleanViewProjMatrixExt.writeToBuffer(CLEAN_VP * 16, DATA);

		cleanViewProjMatrixInvExt.set(viewMatrixInvExt);
		cleanViewProjMatrixInvExt.multiply(cleanProjMatrixInvExt);
		cleanViewProjMatrixInvExt.writeToBuffer(CLEAN_VP_INVERSE * 16, DATA);

		cleanFrustum.prepare(viewMatrix, tickDelta, camera, cleanProjMatrix);
		cleanFrustum.computeCircumCenter(viewMatrixInv, cleanProjMatrixInv);
	}

	static void updateShadow(Camera camera, float tickDelta, TerrainBounds bounds, CelestialObjectOutput skyoutput) {
		computeShadowMatrices(camera, tickDelta, bounds, skyoutput);

		// shadow perspective were computed earlier
		shadowViewMatrixExt.writeToBuffer(SHADOW_VIEW * 16, DATA);
		shadowProjMatrixExt.writeToBuffer(SHADOW_PROJ * 16, DATA);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		// reliable inversion of rotation matrix
		shadowViewMatrixInv.transpose();
		shadowViewMatrixInvExt.writeToBuffer(SHADOW_VIEW_INVERSE * 16, DATA);

		shadowProjMatrixInvExt.set(shadowProjMatrixExt);
		shadowProjMatrixInv.invert();
		shadowProjMatrixInvExt.writeToBuffer(SHADOW_PROJ_INVERSE * 16, DATA);

		shadowViewProjMatrixExt.set(shadowProjMatrixExt);
		shadowViewProjMatrixExt.multiply(shadowViewMatrixExt);
		shadowViewProjMatrixExt.writeToBuffer(SHADOW_VIEW_PROJ * 16, DATA);

		shadowViewProjMatrixInvExt.set(shadowViewMatrixInvExt);
		shadowViewProjMatrixInvExt.multiply(shadowProjMatrixInvExt);
		shadowViewProjMatrixInvExt.writeToBuffer(SHADOW_VIEW_PROJ_INVERSE * 16, DATA);
	}

	/** Depth of the shadow map projection.  Lower values require less offset to avoid artifacts. */
	public static float shadowDepth() {
		return shadowDepth;
	}

	/**
	 * Computes projection that doesn't include nausea or view bob and doesn't have 4X depth like vanilla.
	 */
	public static void computeCleanProjection(Camera camera, float tickDelta) {
		final MinecraftClient mc = MinecraftClient.getInstance();
		final GameRendererExt gx = (GameRendererExt) mc.gameRenderer;
		final float zoom = gx.canvas_zoom();

		cleanProjMatrix.loadIdentity();

		if (zoom != 1.0F) {
			cleanProjMatrixExt.translate(gx.canvas_zoomX(), -gx.canvas_zoomY(), 0.0f);
			cleanProjMatrixExt.scale(zoom, zoom, 1.0F);
		}

		cleanProjMatrix.multiply(Matrix4f.viewboxMatrix(gx.canvas_getFov(camera, tickDelta, true), mc.getWindow().getFramebufferWidth() / mc.getWindow().getFramebufferHeight(), 0.05F, mc.gameRenderer.getViewDistance()));

		cleanProjMatrixInvExt.set(cleanProjMatrixExt);
		cleanProjMatrixInv.invert();
	}

	public static final Matrix4f viewMatrix = new Matrix4f();
	public static final Matrix4fExt viewMatrixExt = (Matrix4fExt) (Object) viewMatrix;
	private static final Matrix4f viewMatrixInv = new Matrix4f();
	private static final Matrix4fExt viewMatrixInvExt = (Matrix4fExt) (Object) viewMatrixInv;

	public static final Matrix4f projMatrix = new Matrix4f();
	public static final Matrix4fExt projMatrixExt = (Matrix4fExt) (Object) projMatrix;
	private static final Matrix4f projMatrixInv = new Matrix4f();
	private static final Matrix4fExt projMatrixInvExt = (Matrix4fExt) (Object) projMatrixInv;

	private static final Matrix4f viewProjMatrix = new Matrix4f();
	private static final Matrix4fExt viewProjMatrixExt = (Matrix4fExt) (Object) viewProjMatrix;
	private static final Matrix4f viewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt viewProjMatrixInvExt = (Matrix4fExt) (Object) viewProjMatrixInv;

	public static final Matrix4f cleanProjMatrix = new Matrix4f();
	public static final Matrix4fExt cleanProjMatrixExt = (Matrix4fExt) (Object) cleanProjMatrix;
	private static final Matrix4f cleanProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt cleanProjMatrixInvExt = (Matrix4fExt) (Object) cleanProjMatrixInv;

	private static final Matrix4f cleanViewProjMatrix = new Matrix4f();
	private static final Matrix4fExt cleanViewProjMatrixExt = (Matrix4fExt) (Object) cleanViewProjMatrix;
	private static final Matrix4f cleanViewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt cleanViewProjMatrixInvExt = (Matrix4fExt) (Object) cleanViewProjMatrixInv;

	public static final Matrix4f shadowViewMatrix = new Matrix4f();
	public static final Matrix4fExt shadowViewMatrixExt = (Matrix4fExt) (Object) shadowViewMatrix;
	private static final Matrix4f shadowViewMatrixInv = new Matrix4f();
	private static final Matrix4fExt shadowViewMatrixInvExt = (Matrix4fExt) (Object) shadowViewMatrixInv;

	public static final Matrix4f shadowProjMatrix = new Matrix4f();
	public static final Matrix4fExt shadowProjMatrixExt = (Matrix4fExt) (Object) shadowProjMatrix;
	private static final Matrix4f shadowProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt shadowProjMatrixInvExt = (Matrix4fExt) (Object) shadowProjMatrixInv;

	public static final Matrix4f shadowViewProjMatrix = new Matrix4f();
	public static final Matrix4fExt shadowViewProjMatrixExt = (Matrix4fExt) (Object) shadowViewProjMatrix;
	private static final Matrix4f shadowViewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt shadowViewProjMatrixInvExt = (Matrix4fExt) (Object) shadowViewProjMatrixInv;

	public static final Matrix3f viewNormalMatrix = new Matrix3f();

	private static float shadowDepth;

	// frustum without nausea or view bob
	public static final FastFrustum cleanFrustum = new FastFrustum();

	private static final int VIEW = 0;
	private static final int VIEW_INVERSE = 1;
	private static final int VIEW_LAST = 2;
	private static final int PROJ = 3;
	private static final int PROJ_INVERSE = 4;
	private static final int PROJ_LAST = 5;
	private static final int VP = 6;
	private static final int VP_INVERSE = 7;
	private static final int VP_LAST = 8;

	private static final int SHADOW_VIEW = 9;
	private static final int SHADOW_VIEW_INVERSE = 10;
	private static final int SHADOW_PROJ = 11;
	private static final int SHADOW_PROJ_INVERSE = 12;
	private static final int SHADOW_VIEW_PROJ = 13;
	private static final int SHADOW_VIEW_PROJ_INVERSE = 14;

	private static final int CLEAN_PROJ = 15;
	private static final int CLEAN_PROJ_INVERSE = 16;
	private static final int CLEAN_PROJ_LAST = 17;
	private static final int CLEAN_VP = 18;
	private static final int CLEAN_VP_INVERSE = 19;
	private static final int CLEAN_VP_LAST = 20;

	public static final int COUNT = 24;
	public static final FloatBuffer DATA = BufferUtils.createFloatBuffer(COUNT * 16);
}
