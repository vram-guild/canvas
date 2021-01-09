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

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.pipeline.PipelineManager;

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

	static int i = 0;
	@SuppressWarnings("resource")
	private static void computeShadowMatrices(Camera camera) {
		final float viewDist = MinecraftClient.getInstance().gameRenderer.getViewDistance();

		// construct rotation matrix from sun position
		shadowViewMatrixExt.loadIdentity();

		// WIP: automatically scale this and ortho frustum depth for best precision
		// move camera back from frustum center to see whole scene
		// Is Z-axis only because we are now pointing towards the center
		// (Steps are reverse order for matrix multiply)
		shadowViewMatrixExt.translate(0f, 0f, -viewDist);

		// Rotate to fit image mostly into the size of our regular framebuffer
		// We don't have to use that size for the shadow map, but it tends to fit pretty well.
		// WIP: use a fixed res, probably square size, and cascaded levels
		shadowViewMatrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(camera.getYaw()));

		// Rotate for position of sun
		shadowViewMatrix.multiply(Vector3f.POSITIVE_Y.getRadialQuaternion((float) (Math.PI * 0.5) - WorldDataManager.skyShadowRotationRadiansZ));

		// Rotate to view from top down
		shadowViewMatrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90));

		// reverse Z-axis direction to align w/ opengl view
		shadowViewMatrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));

		// translate to middle of frustum
		final Vec3d cv = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
		shadowViewMatrixExt.translate(
				(float) (cv.x * viewDist * -0.5),
				(float) (cv.y * viewDist * -0.5),
				(float) (cv.z * viewDist * -0.5));

		// orthographic projection
		final float near = 0.05f;
		final float far = viewDist * 2;

		// WIP: improve scaling based on bounds of rendered chunks
		final float scale = viewDist * 2 / PipelineManager.width();

		shadowProjMatrixExt.a00(2f / scale / PipelineManager.width());
		shadowProjMatrixExt.a11(2f / scale / PipelineManager.height());
		shadowProjMatrixExt.a22(-2f / (far - near));
		shadowProjMatrixExt.a03(0.0f);
		shadowProjMatrixExt.a13(0.0f);
		shadowProjMatrixExt.a23(-((far + near) / (far - near)));
		shadowProjMatrixExt.a33(1f);
	}

	/**
	 * Depends on WorldDataManager and should be called after it updates.
	 */
	public static void update(MatrixState val, MatrixStack.Entry view, Matrix4f projectionMatrix, Camera camera) {
		assert val != null;
		current = val;

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
		projMatrixInvExt.invertProjection();
		projMatrixInvExt.writeToBuffer(PROJ_INVERSE * 16, DATA);

		viewProjMatrixExt.set(projMatrixExt);
		viewProjMatrixExt.multiply(viewMatrixExt);
		viewProjMatrixExt.writeToBuffer(VP * 16, DATA);

		viewProjMatrixInvExt.set(viewMatrixInvExt);
		viewProjMatrixInvExt.multiply(projMatrixInvExt);
		viewProjMatrixInvExt.writeToBuffer(VP_INVERSE * 16, DATA);

		// shadow perspective
		computeShadowMatrices(camera);
		shadowViewMatrixExt.writeToBuffer(SHADOW_VIEW * 16, DATA);
		shadowProjMatrixExt.writeToBuffer(SHADOW_PROJ * 16, DATA);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		// reliable inversion of rotation matrix
		shadowViewMatrixInv.transpose();
		shadowViewMatrixInvExt.writeToBuffer(SHADOW_VIEW_INVERSE * 16, DATA);

		shadowProjMatrixInvExt.set(shadowProjMatrixExt);
		shadowProjMatrixInvExt.invertProjection();
		shadowProjMatrixInvExt.writeToBuffer(SHADOW_PROJ_INVERSE * 16, DATA);

		shadowViewProjMatrixExt.set(shadowProjMatrixExt);
		shadowViewProjMatrixExt.multiply(shadowViewMatrixExt);
		shadowViewProjMatrixExt.writeToBuffer(SHADOW_VIEW_PROJ * 16, DATA);

		shadowViewProjMatrixInvExt.set(shadowViewMatrixInvExt);
		shadowViewProjMatrixInvExt.multiply(shadowProjMatrixInvExt);
		shadowViewProjMatrixInvExt.writeToBuffer(SHADOW_VIEW_PROJ_INVERSE * 16, DATA);
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
