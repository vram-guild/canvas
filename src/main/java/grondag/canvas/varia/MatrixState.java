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

import static grondag.canvas.varia.WorldDataManager.frustumCenter;
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

	private static void computeShadowMatrices(Camera camera, float tickDelta, TerrainBounds bounds) {
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


		final float radius = straightFrustum.circumRadius();
		final double sqRadius = (double) radius * (double) radius;

		// look at world origin to compute relative scale of world XYZ
		worldShadowViewExt.lookAt(
				WorldDataManager.skyLightVector.getX() * radius,
				WorldDataManager.skyLightVector.getY() * radius,
				WorldDataManager.skyLightVector.getZ() * radius,
				0,
				0,
				0,
				0.0f, 0.0f, 1.0f);

		final Vector4f testVec = new Vector4f();

		testVec.set(radius, 0, 0, 1.0f);
		testVec.transform(worldShadowView);
		final double xProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());

		testVec.set(0, radius, 0, 1.0f);
		testVec.transform(worldShadowView);
		final double yProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());

		testVec.set(0, 0, radius, 1.0f);
		testVec.transform(worldShadowView);
		final double zProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());

		final double xClampedRadius = Math.round(sqRadius / xProjectedRadius);
		final double yClampedRadius = Math.round(sqRadius / yProjectedRadius);
		final double zClampedRadius = Math.round(sqRadius / zProjectedRadius);

		final double xWorldPerPixel = 2.0 * sqRadius / xProjectedRadius / Pipeline.skyShadowSize;
		final double yWorldPerPixel = 2.0 * sqRadius / yProjectedRadius / Pipeline.skyShadowSize;
		final double zWorldPerPixel = 2.0 * sqRadius / zProjectedRadius / Pipeline.skyShadowSize;

		//System.out.println(String.format("%f  %f %f", xWorldPerPixel, yWorldPerPixel, zWorldPerPixel));

		final float mx = straightFrustum.circumCenterX();
		final float my = straightFrustum.circumCenterY();
		final float mz = straightFrustum.circumCenterZ();

		final double fwx = WorldDataManager.cameraXd + mx;
		final double fwy = WorldDataManager.cameraYd + my;
		final double fwz = WorldDataManager.cameraZd + mz;

		// clamp to pixel boundary
		final double cfwx = Math.floor(fwx / xWorldPerPixel) * xWorldPerPixel;
		final double cfwy = Math.floor(fwy / yWorldPerPixel) * yWorldPerPixel;
		final double cfwz = Math.floor(fwz / zWorldPerPixel) * zWorldPerPixel;

		final double fdx = fwx - cfwx;
		final double fdy = fwy - cfwy;
		final double fdz = fwz - cfwz;

		testVec.set((float) fdx, (float) fdy, (float) fdz, 1.0f);
		testVec.transform(worldShadowView);

		final float dx = testVec.getX();
		final float dy = testVec.getY();

		lastSkyLightPosition.set(skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ());

		skyLightPosition.set(
				mx + skyLightVector.getX() * radius,
				my + skyLightVector.getY() * radius,
				mz + skyLightVector.getZ() * radius);

		// Look from skylight towards center of the view frustum in camera space
		shadowViewMatrixExt.lookAt(
				WorldDataManager.skyLightPosition.getX(),
				WorldDataManager.skyLightPosition.getY(),
				WorldDataManager.skyLightPosition.getZ(),
				mx, my, mz,
				0.0f, 0.0f, 1.0f);

		bounds.computeViewBounds(shadowViewMatrixExt, WorldDataManager.cameraX, WorldDataManager.cameraY, WorldDataManager.cameraZ);

		// Construct ortho matrix using bounding sphere box computed above
		// Should give us a consistent size each frame until the sun moves.
		// We use actual geometry depth to give better precision on Z.
		// Z axis inverted to match depth axis in OpenGL
		shadowProjMatrixExt.setOrtho(
			-radius - dx, radius - dx,
			-radius - dy, radius - dy,
			-bounds.maxViewZ(), -bounds.minViewZ());

		shadowDepth = Math.abs(bounds.maxViewZ() - bounds.minViewZ());
	}

	static void update(MatrixStack.Entry view, Matrix4f projectionMatrix, Camera camera, float tickDelta, TerrainBounds bounds) {
		// write values for prior frame before updating
		viewMatrixExt.writeToBuffer(VIEW_LAST * 16, DATA);
		projMatrixExt.writeToBuffer(PROJ_LAST * 16, DATA);
		viewProjMatrixExt.writeToBuffer(VP_LAST * 16, DATA);

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
		//projMatrixInvExt.invertProjection();
		projMatrixInvExt.writeToBuffer(PROJ_INVERSE * 16, DATA);

		viewProjMatrixExt.set(projMatrixExt);
		viewProjMatrixExt.multiply(viewMatrixExt);
		viewProjMatrixExt.writeToBuffer(VP * 16, DATA);

		viewProjMatrixInvExt.set(viewMatrixInvExt);
		viewProjMatrixInvExt.multiply(projMatrixInvExt);
		viewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, DATA);

		computeStraightProjection(camera, tickDelta);

		// WIP: write these to buffer, along with straight projection?
		straightViewProjMatrixExt.set(straightProjMatrixExt);
		straightViewProjMatrixExt.multiply(viewMatrixExt);
		//straightViewProjMatrixExt.writeToBuffer(VP * 16, DATA);

		straightViewProjMatrixInvExt.set(viewMatrixInvExt);
		straightViewProjMatrixInvExt.multiply(straightProjMatrixInvExt);
		//straightViewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, DATA);

		straightFrustum.prepare(viewMatrix, tickDelta, camera, straightProjMatrix);
		straightFrustum.computeCircumCenter(viewMatrixInv, straightProjMatrixInv);

		frustumCenter.set(straightFrustum.circumCenterX(), straightFrustum.circumCenterY(), straightFrustum.circumCenterZ());
	}

	static void updateShadow(Camera camera, float tickDelta, TerrainBounds bounds) {
		computeShadowMatrices(camera, tickDelta, bounds);

		// shadow perspective were computed earlier
		shadowViewMatrixExt.writeToBuffer(SHADOW_VIEW * 16, DATA);
		shadowProjMatrixExt.writeToBuffer(SHADOW_PROJ * 16, DATA);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		// reliable inversion of rotation matrix
		shadowViewMatrixInv.transpose();
		shadowViewMatrixInvExt.writeToBuffer(SHADOW_VIEW_INVERSE * 16, DATA);

		shadowProjMatrixInvExt.set(shadowProjMatrixExt);
		shadowProjMatrixInv.invert();
		//shadowProjMatrixInvExt.invertProjection();
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
	public static void computeStraightProjection(Camera camera, float tickDelta) {
		final MinecraftClient mc = MinecraftClient.getInstance();
		final GameRendererExt gx = (GameRendererExt) mc.gameRenderer;
		final float zoom = gx.canvas_zoom();

		straightProjMatrix.loadIdentity();

		if (zoom != 1.0F) {
			straightProjMatrixExt.translate(gx.canvas_zoomX(), -gx.canvas_zoomY(), 0.0f);
			straightProjMatrixExt.scale(zoom, zoom, 1.0F);
		}

		straightProjMatrix.multiply(Matrix4f.viewboxMatrix(gx.canvas_getFov(camera, tickDelta, true), mc.getWindow().getFramebufferWidth() / mc.getWindow().getFramebufferHeight(), 0.05F, mc.gameRenderer.getViewDistance()));

		straightProjMatrixInvExt.set(straightProjMatrixExt);
		straightProjMatrixInv.invert();
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

	public static final Matrix4f straightProjMatrix = new Matrix4f();
	public static final Matrix4fExt straightProjMatrixExt = (Matrix4fExt) (Object) straightProjMatrix;
	private static final Matrix4f straightProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt straightProjMatrixInvExt = (Matrix4fExt) (Object) straightProjMatrixInv;

	private static final Matrix4f straightViewProjMatrix = new Matrix4f();
	private static final Matrix4fExt straightViewProjMatrixExt = (Matrix4fExt) (Object) straightViewProjMatrix;
	private static final Matrix4f straightViewProjMatrixInv = new Matrix4f();
	private static final Matrix4fExt straightViewProjMatrixInvExt = (Matrix4fExt) (Object) straightViewProjMatrixInv;

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

	private static final Matrix4f worldShadowView = new Matrix4f();
	private static final Matrix4fExt worldShadowViewExt = (Matrix4fExt) (Object) worldShadowView;

	private static final Vector3f texelAlignmentPos = new Vector3f();
	private static float shadowDepth;

	// frustum without nausea or view bob
	public static final FastFrustum straightFrustum = new FastFrustum();

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

	public static final int COUNT = 15;
	public static final FloatBuffer DATA = BufferUtils.createFloatBuffer(COUNT * 16);
}
