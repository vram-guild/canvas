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
import static grondag.canvas.varia.WorldDataManager.lastFrustumCenter;
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

	@SuppressWarnings({ "resource", "unused" })
	private static void computeShadowMatricesWip(Camera camera, float tickDelta, TerrainBounds bounds) {
		final float viewDist = MinecraftClient.getInstance().gameRenderer.getViewDistance();

		straightFrustum.prepare(viewMatrix, tickDelta, camera, straightProjMatrix);

		final Vector4f corner = new Vector4f();

		// near lower left
		corner.set(-1f, -1f, -1f, 1f);
		corner.transform(straightProjMatrixInv);

		float nx0 = corner.getX() / corner.getW();
		float ny0 = corner.getY() / corner.getW();
		float nz0 = corner.getZ() / corner.getW();

		corner.set(nx0, ny0, nz0, 1f);
		corner.transform(viewMatrixInv);

		nx0 = corner.getX();
		ny0 = corner.getY();
		nz0 = corner.getZ();

		// near top right
		corner.set(1f, 1f, -1f, 1f);
		corner.transform(straightProjMatrixInv);

		float nx1 = corner.getX() / corner.getW();
		float ny1 = corner.getY() / corner.getW();
		float nz1 = corner.getZ() / corner.getW();

		corner.set(nx1, ny1, nz1, 1f);
		corner.transform(viewMatrixInv);

		nx1 = corner.getX();
		ny1 = corner.getY();
		nz1 = corner.getZ();

		// far lower left
		corner.set(-1f, -1f, 1f, 1f);
		corner.transform(straightProjMatrixInv);

		float fx0 = corner.getX() / corner.getW();
		float fy0 = corner.getY() / corner.getW();
		float fz0 = corner.getZ() / corner.getW();

		corner.set(fx0, fy0, fz0, 1f);
		corner.transform(viewMatrixInv);

		fx0 = corner.getX();
		fy0 = corner.getY();
		fz0 = corner.getZ();

		// far top right
		corner.set(1f, 1f, 1f, 1f);
		corner.transform(straightProjMatrixInv);

		float fx1 = corner.getX() / corner.getW();
		float fy1 = corner.getY() / corner.getW();
		float fz1 = corner.getZ() / corner.getW();

		corner.set(fx1, fy1, fz1, 1f);
		corner.transform(viewMatrixInv);

		fx1 = corner.getX();
		fy1 = corner.getY();
		fz1 = corner.getZ();

		final float mx = (nx0 + nx1 + fx0 + fx1) * 0.25f;
		final float my = (ny0 + ny1 + fy0 + fy1) * 0.25f;
		final float mz = (nz0 + nz1 + fz0 + fz1) * 0.25f;

		WorldDataManager.frustumCenter.set(mx, my, mz);

		// WIP: compute radius 1X
		final float radius = (float) Math.sqrt((mx - fx1) * (mx - fx1) + (my - fy1) * (my - fy1) + (mz - fz1) * (mz - fz1));

		lastSkyLightPosition.set(skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ());

		skyLightPosition.set(
				mx + skyLightVector.getX() * radius,
				my + skyLightVector.getY() * radius,
				mz + skyLightVector.getZ() * radius);

		// don't move skylight unless it is at least a 1 pixel change in 1 axis

		// Look from skylight towards center of the view frustum in camera space
		shadowViewMatrixExt.lookAt(
				lastSkyLightPosition.getX(),
				lastSkyLightPosition.getY(),
				lastSkyLightPosition.getZ(),
				mx, my, mz,
				0.0f, 0.0f, 1.0f);

		final Vector4f last = new Vector4f(lastSkyLightPosition.getX(), lastSkyLightPosition.getY(), lastSkyLightPosition.getZ(), 1.0f);
		final Vector4f current = new Vector4f(skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ(), 1.0f);
		last.transform(shadowViewMatrix);
		current.transform(shadowViewMatrix);

		final float qdx = current.getX() - last.getX();
		final float qdy = current.getY() - last.getY();

		final float pixelSize = radius / Pipeline.skyShadowSize;
		final float qx = (float) (Math.floor(qdx / pixelSize) * pixelSize);
		final float qy = (float) (Math.floor(qdy / pixelSize) * pixelSize);
		//final float qz = (float) (Math.floor(qdz / 0.5f) * 0.5f);

		current.set(last.getX() + qx, last.getY() + qy, radius, 1.0f);

		shadowViewMatrix.invert();

		current.transform(shadowViewMatrix);

		skyLightPosition.set(
				current.getX(),
				current.getY(),
				current.getZ());

		shadowViewMatrixExt.lookAt(
				skyLightPosition.getX(),
				skyLightPosition.getY(),
				skyLightPosition.getZ(),
				mx, my, mz,
				0.0f, 0.0f, 1.0f);

		worldShadowViewExt.lookAt(
				WorldDataManager.skyLightVector.getX() * viewDist * 2,
				WorldDataManager.skyLightVector.getY() * viewDist * 2,
				WorldDataManager.skyLightVector.getZ() * viewDist * 2,
				0,
				0,
				0,
				0.0f, 0.0f, 1.0f);
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
		computeBounds(radius, mx, my, mz);

		// To avoid precision issues at the edge of the world, use a world boundary
		// that is relatively close - keeping them at regular intervals.
		final int ox = (int) Math.floor(WorldDataManager.cameraXd) & 0xFFFFFF00;
		final int oy = (int) Math.floor(WorldDataManager.cameraYd) & 0xFFFFFF00;
		final int oz = (int) Math.floor(WorldDataManager.cameraZd) & 0xFFFFFF00;

		texelAlignmentPos.set(
				(float) (WorldDataManager.cameraXd - ox + mx),
				(float) (WorldDataManager.cameraYd - oy + my),
				(float) (WorldDataManager.cameraZd - oz + mz));

		worldShadowViewExt.fastTransform(texelAlignmentPos);

		final float xSpan = x1 - x0;
		final float ySpan = y1 - y0;
		final float xWorldUnitsPerTexel = xSpan / Pipeline.skyShadowSize;
		final float yWorldUnitsPerTexel = ySpan / Pipeline.skyShadowSize;

		final float clampedX = Math.round((texelAlignmentPos.getX()) / xWorldUnitsPerTexel) * xWorldUnitsPerTexel;
		final float clampedY = Math.round((texelAlignmentPos.getY()) / yWorldUnitsPerTexel) * yWorldUnitsPerTexel;
		final float dx = texelAlignmentPos.getX() - clampedX;
		final float dy = texelAlignmentPos.getY() - clampedY;

		bounds.computeViewBounds(shadowViewMatrixExt, WorldDataManager.cameraX, WorldDataManager.cameraY, WorldDataManager.cameraZ);

		// Construct ortho matrix using bounding sphere box computed above
		// Should give us a consistent size each frame until the sun moves.
		// We use actual geometry depth to give better precision on Z.
		// Z axis inverted to match depth axis in OpenGL
		shadowProjMatrixExt.setOrtho(
			x0 - dx, x1 - dx,
			y0 - dy, y1 - dy,
			-bounds.maxViewZ(), -bounds.minViewZ());

		shadowDepth = Math.abs(bounds.maxViewZ() - bounds.minViewZ());
	}

	// WIP: remove
	@SuppressWarnings({ "resource"})
	private static void computeShadowMatrices(Camera camera, float tickDelta, TerrainBounds bounds) {
		final float viewDist = MinecraftClient.getInstance().gameRenderer.getViewDistance();

		final float mx = straightFrustum.circumCenterX();
		final float my = straightFrustum.circumCenterY();
		final float mz = straightFrustum.circumCenterZ();
		final float radius = straightFrustum.circumRadius();

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

		worldShadowViewExt.lookAt(
				WorldDataManager.skyLightVector.getX() * viewDist * 2,
				WorldDataManager.skyLightVector.getY() * viewDist * 2,
				WorldDataManager.skyLightVector.getZ() * viewDist * 2,
				0,
				0,
				0,
				0.0f, 0.0f, 1.0f);

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
		computeBounds(radius, mx, my, mz);

		// To avoid precision issues at the edge of the world, use a world boundary
		// that is relatively close - keeping them at regular intervals.
		final int ox = (int) Math.floor(WorldDataManager.cameraXd) & 0xFFFFFF00;
		final int oy = (int) Math.floor(WorldDataManager.cameraYd) & 0xFFFFFF00;
		final int oz = (int) Math.floor(WorldDataManager.cameraZd) & 0xFFFFFF00;

		texelAlignmentPos.set(
				(float) (WorldDataManager.cameraXd - ox + mx),
				(float) (WorldDataManager.cameraYd - oy + my),
				(float) (WorldDataManager.cameraZd - oz + mz));

		worldShadowViewExt.fastTransform(texelAlignmentPos);

		final float xSpan = x1 - x0;
		final float ySpan = y1 - y0;
		final float xWorldUnitsPerTexel = xSpan / Pipeline.skyShadowSize;
		final float yWorldUnitsPerTexel = ySpan / Pipeline.skyShadowSize;

		final float clampedX = Math.round((texelAlignmentPos.getX()) / xWorldUnitsPerTexel) * xWorldUnitsPerTexel;
		final float clampedY = Math.round((texelAlignmentPos.getY()) / yWorldUnitsPerTexel) * yWorldUnitsPerTexel;
		final float dx = texelAlignmentPos.getX() - clampedX;
		final float dy = texelAlignmentPos.getY() - clampedY;

		bounds.computeViewBounds(shadowViewMatrixExt, WorldDataManager.cameraX, WorldDataManager.cameraY, WorldDataManager.cameraZ);

		// Construct ortho matrix using bounding sphere box computed above
		// Should give us a consistent size each frame until the sun moves.
		// We use actual geometry depth to give better precision on Z.
		// Z axis inverted to match depth axis in OpenGL
		shadowProjMatrixExt.setOrtho(
			x0 - dx, x1 - dx,
			y0 - dy, y1 - dy,
			-bounds.maxViewZ(), -bounds.minViewZ());

		shadowDepth = Math.abs(bounds.maxViewZ() - bounds.minViewZ());
	}

	private static void computeBounds(float radius, float fcx, float fcy, float fcz) {
		boundsPos.set(fcx - radius, fcy - radius, fcz - radius);
		shadowViewMatrixExt.fastTransform(boundsPos);

		x0 = boundsPos.getX();
		x1 = x0;
		y0 = boundsPos.getY();
		y1 = y0;

		boundsPos.set(fcx - radius, fcy - radius, fcz + radius);
		computeBoundsInner();

		boundsPos.set(fcx - radius, fcy + radius, fcz - radius);
		computeBoundsInner();

		boundsPos.set(fcx - radius, fcy + radius, fcz + radius);
		computeBoundsInner();

		boundsPos.set(fcx + radius, fcy - radius, fcz - radius);
		computeBoundsInner();

		boundsPos.set(fcx + radius, fcy - radius, fcz + radius);
		computeBoundsInner();

		boundsPos.set(fcx + radius, fcy + radius, fcz - radius);
		computeBoundsInner();

		boundsPos.set(fcx + radius, fcy + radius, fcz + radius);
		computeBoundsInner();
	}

	private static void computeBoundsInner() {
		shadowViewMatrixExt.fastTransform(boundsPos);

		final float x = boundsPos.getX();

		if (x < x0) {
			x0 = x;
		} else if (x > x1) {
			x1 = x;
		}

		final float y = boundsPos.getY();

		if (y < y0) {
			y0 = y;
		} else if (y > y1) {
			y1 = y;
		}
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

		// WIP: write these to buffer, along with straight projection
		straightViewProjMatrixExt.set(straightProjMatrixExt);
		straightViewProjMatrixExt.multiply(viewMatrixExt);
		//straightViewProjMatrixExt.writeToBuffer(VP * 16, DATA);

		straightViewProjMatrixInvExt.set(viewMatrixInvExt);
		straightViewProjMatrixInvExt.multiply(straightProjMatrixInvExt);
		//straightViewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, DATA);

		straightFrustum.prepare(viewMatrix, tickDelta, camera, straightProjMatrix);
		straightFrustum.computeCircumCenter(viewMatrixInv, straightProjMatrixInv);

		lastFrustumCenter.set(frustumCenter.getX(), frustumCenter.getY(), frustumCenter.getZ());
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
	private static final Vector3f boundsPos = new Vector3f();
	private static float x0, y0, x1, y1;
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
