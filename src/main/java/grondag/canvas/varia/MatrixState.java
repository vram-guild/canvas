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

	private static Vector3f lastSkylightVector = new Vector3f();
	private static float xCurrent, yCurrent, zCurrent;
	private static float bestErr, xBest, yBest, zBest;
	private static float worldPerPixel;
	private static int radius;
	private static final Vector4f testVec = new Vector4f();

	//private static float pxLast, pyLast, pzLast, dpxLast, dpyLast, dpzLast;

	//private static double lastErr;
	private static float lastX, lastY, lastZ;
	//private static boolean didErrIncrease;

	@SuppressWarnings("resource")
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

		radius = (int) (MinecraftClient.getInstance().gameRenderer.getViewDistance() / 2); //Math.round(straightFrustum.circumRadius());
		worldPerPixel = 2f * radius / Pipeline.skyShadowSize;
		final double radiusPixels = Pipeline.skyShadowSize / 2.0;

		final double x = skyLightVector.getX();
		final double y = skyLightVector.getY();
		final double z = skyLightVector.getZ();

		//System.out.println();
		//System.out.println();

		// 00 01 02 03
		// 10 11 12 13
		// 20 21 22 23
		// 30 31 32 33

		shadowViewMatrixExt.lookAt(
				(float) (x * radius),
				(float) (y * radius),
				(float) (z * radius),
				0,
				0,
				0,
				0.0f, 0.0f, 1.0f);

		//System.out.println(shadowViewMatrixExt.toString());

		//		final double apx = Math.sqrt(1 - x * x);
		//		final double apy = Math.sqrt(1 - y * y);
		//		final double apz = Math.sqrt(1 - z * z);
		//
		//		final double sxr = Math.round(apx * radiusPixels) / radiusPixels;
		//		final double syr = Math.round(apy * radiusPixels) / radiusPixels;
		//		final double szr = Math.round(apz * radiusPixels) / radiusPixels;
		//
		//		final double xf = Math.sqrt(1 - sxr * sxr);
		//		final double yf = Math.sqrt(1 - syr * syr);
		//		final double zf = Math.sqrt(1 - szr * szr);
		//
		//		final double sx = Math.signum(x) * xf;
		//		final double sy = Math.signum(y) * yf;
		//		final double sz = Math.signum(z) * zf;
		//
		//		final double n = 1.0 / Math.sqrt(sx * sx + sy * sy + sz * sz);
		//
		//		final double nsx = sx * n;
		//		final double nsy = sy * n;
		//		final double nsz = sz * n;

		//System.out.println(nsx * nsx + nsy * nsy + nsz * nsz);

		//xBest = (float) (nsx * radius);
		//yBest = (float) (nsy * radius);
		//zBest = (float) (nsz * radius);

		xBest = (float) (x * radius);
		yBest = (float) (y * radius);
		zBest = (float) (z * radius);

		//if (xCurrent != xBest || yCurrent != yBest || zCurrent != zBest) {
			//System.out.println(String.format("x:%f  y:%f  z:%f",
			//		xBest / worldPerPixel, yBest / worldPerPixel, zBest / worldPerPixel));

			//System.out.println(sxt + "  " + syt + "  " + szt);
			//System.out.println(sx * radiusPixels + "  " + syt * radiusPixels + "  " + szt * radiusPixels);
			//System.out.println();

		xCurrent = xBest;
		yCurrent = yBest;
		zCurrent = zBest;

		shadowViewMatrixExt.lookAt(
				xCurrent,
				yCurrent,
				zCurrent,
				0,
				0,
				0,
				0.0f, 0.0f, 1.0f);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		shadowViewMatrixInv.invert();

		final float a = shadowViewMatrixExt.a01();
		final float ca = (float) (Math.round(a * radiusPixels) / radiusPixels);

		final float b = Math.abs(shadowViewMatrixExt.a00());
		final float cb = (float) (Math.round(b * radiusPixels) / radiusPixels);

		shadowViewMatrixExt.a00(Math.signum(shadowViewMatrixExt.a00()) * cb);
		shadowViewMatrixExt.a01(ca);
		shadowViewMatrixExt.a02(0);
		shadowViewMatrixExt.a03(0);

		shadowViewMatrixExt.a10(0);
		shadowViewMatrixExt.a11(0);
		shadowViewMatrixExt.a12(1);
		shadowViewMatrixExt.a13(0);

		shadowViewMatrixExt.a20(ca);
		shadowViewMatrixExt.a21(Math.signum(shadowViewMatrixExt.a21()) * cb);
		shadowViewMatrixExt.a22(0);
		shadowViewMatrixExt.a23(shadowViewMatrixExt.a23());

		shadowViewMatrixExt.a30(0);
		shadowViewMatrixExt.a31(0);
		shadowViewMatrixExt.a32(0);
		shadowViewMatrixExt.a33(1);

		testVec.set(0, 0, 0, 1.0f);
		testVec.transform(shadowViewMatrixInv);

		xBest = testVec.getX();
		yBest = testVec.getY();
		zBest = testVec.getZ();
		xCurrent = xBest;
		yCurrent = yBest;
		zCurrent = zBest;

		testVec.set(radius, radius, radius, 1.0f);
		testVec.transform(shadowViewMatrix);

		final float px = testVec.getX() / worldPerPixel;
		final float py = testVec.getY() / worldPerPixel;
		final float pz = testVec.getZ() / worldPerPixel;

		if (Math.abs(px - lastX) > 0.001f || Math.abs(py - lastY) > 0.001f || Math.abs(pz - lastZ) > 0.001f) {
			lastX = px;
			lastY = py;
			lastZ = pz;
			//			System.out.println("");
			//			System.out.println("START");
			//			System.out.println(shadowViewMatrixExt.toString());
			//			System.out.println(testVec.getX() / worldPerPixel + "   " + testVec.getY() / worldPerPixel + "    " + testVec.getZ() / worldPerPixel);
			//
			//			shadowViewMatrixExt.lookAt(
			//					xCurrent,
			//					yCurrent,
			//					zCurrent,
			//					0,
			//					0,
			//					0,
			//					0.0f, 0.0f, 1.0f);
			//
			//			System.out.println(shadowViewMatrixExt.toString());
		}

		lastSkylightVector.set(xCurrent, yCurrent, zCurrent);
		skyLightVector.set(xCurrent, yCurrent, zCurrent);

		computeShadowMatricesInner(camera, radius, bounds);
	}

	//	private static void computeShadowMatricesMeh(Camera camera, float tickDelta, TerrainBounds bounds) {
	//		// We need to keep the skylight projection consistently aligned to
	//		// pixels in the shadowmap texture.  The alignment must be to world
	//		// coordinates in the x/y axis of the skylight perspective.
	//		// Both the frustum center and light position move as the camera moves,
	//		// which causes shimmering if we don't adjust for this movement.
	//
	//		// Because all of our coordinates and matrices at this point are relative to camera,
	//		// we can't test use them for the alignment to world coordinates.
	//		// So we compute the position of the frustum center in world space in a
	//		// projection centered on world origin. Depth doesn't matter here.
	//		// As the camera moves, the x/y distance of its position from the origin
	//		// indicate how much we need to translate the shadowmap projection to maintain alignment.
	//
	//		// Frustum center is at most half view distance away from each frustum plane
	//		// Expanding in each direction by that much should enclose the visible scene
	//		// (Approximate because view frustum isn't a simple box.)
	//		// Note the Y-axis pffset is inverted because MC Y is inverted relative to OpenGL/matrix transform
	//
	//		// To avoid precision issues at the edge of the world, use a world boundary
	//		// that is relatively close - keeping them at regular intervals.
	//
	//		radius = (int) (MinecraftClient.getInstance().gameRenderer.getViewDistance() / 2); //Math.round(straightFrustum.circumRadius());
	//		worldPerPixel = 2f * radius / Pipeline.skyShadowSize;
	//
	//		final int dist = radius * 2;
	//
	//		final float x = skyLightVector.getX() * dist;
	//		final float y = skyLightVector.getY() * dist;
	//		final float z = skyLightVector.getZ() * dist;
	//
	//		shadowViewMatrixExt.lookAt(
	//				x,
	//				y,
	//				z,
	//				0,
	//				0,
	//				0,
	//				0.0f, 0.0f, 1.0f);
	//
	//		testVec.set(radius, 0, 0, 1.0f);
	//		testVec.transform(shadowViewMatrix);
	//		// WIP: this ain't right - will not be for x in light space
	//		final double xProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
	//
	//		testVec.set(0, radius, 0, 1.0f);
	//		testVec.transform(shadowViewMatrix);
	//		final double yProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
	//
	//		testVec.set(0, 0, radius, 1.0f);
	//		testVec.transform(shadowViewMatrix);
	//		final double zProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
	//
	//		final int sqRadius = radius * radius;
	//		final double xWorldPerPixel = 2.0 * sqRadius / Math.round(xProjectedRadius) / Pipeline.skyShadowSize;
	//		final double yWorldPerPixel = 2.0 * sqRadius / Math.round(yProjectedRadius) / Pipeline.skyShadowSize;
	//		final double zWorldPerPixel = 2.0 * sqRadius / Math.round(zProjectedRadius) / Pipeline.skyShadowSize;
	//
	//		testVec.set(radius, radius, radius, 1.0f);
	//		testVec.transform(shadowViewMatrix);
	//		final float rx = testVec.getX();
	//		final float ry = testVec.getY();
	//		final float rz = testVec.getZ();
	//
	//		final float cx = (float) (Math.round(rx / xWorldPerPixel) * zWorldPerPixel);
	//		final float cy = (float) (Math.round(ry / yWorldPerPixel) * yWorldPerPixel);
	//		final float cz = (float) (Math.round(rz / zWorldPerPixel) * zWorldPerPixel);
	//
	//		final float dx = cx - rx;
	//		final float dy = cy - ry;
	//		final float dz = cz - rz;
	//
	//		//final float err = dx * dx + dy * dy + dz * dz;
	//		//final float err = dx * dx;
	//		final float err = Math.abs(dx / worldPerPixel);
	//
	//		//		if (err != lastErr) {
	//		//			System.out.println(err);
	//		//		}
	//
	//		//xBest = xCurrent;
	//		//yBest = yCurrent;
	//		//zBest = zCurrent;
	//
	//		xBest = (float) (Math.round(x / xWorldPerPixel) * xWorldPerPixel);
	//		yBest = (float) (Math.round(y / yWorldPerPixel) * yWorldPerPixel);
	//		zBest = (float) (Math.round(z / zWorldPerPixel) * zWorldPerPixel);
	//
	//		//		if (err <= 0.02) {
	//		//			xBest = x;
	//		//			yBest = y;
	//		//			zBest = z;
	//		//		}
	//
	//		//		if (err > lastErr) {
	//		//			if (!didErrIncrease) {
	//		//				xBest = lastX;
	//		//				yBest = lastY;
	//		//				zBest = lastZ;
	//		//			}
	//		//
	//		//			didErrIncrease = true;
	//		//		} else {
	//		//			lastX = x;
	//		//			lastY = y;
	//		//			lastZ = z;
	//		//			didErrIncrease = false;
	//		//		}
	//		//
	//		//		lastErr = err;
	//
	//		if (xCurrent != xBest || yCurrent != yBest || zCurrent != zBest) {
	//			System.out.println(String.format("x:%f  y:%f  z:%f",
	//					xBest / worldPerPixel, yBest / worldPerPixel, zBest / worldPerPixel));
	//
	//			xCurrent = xBest;
	//			yCurrent = yBest;
	//			zCurrent = zBest;
	//
	//			shadowViewMatrixExt.lookAt(
	//					xCurrent,
	//					yCurrent,
	//					zCurrent,
	//					0,
	//					0,
	//					0,
	//					0.0f, 0.0f, 1.0f);
	//
	//			testVec.set(radius, radius, radius, 1.0f);
	//			testVec.transform(shadowViewMatrix);
	//
	//			final float px = testVec.getX();
	//			final float py = testVec.getY();
	//			final float pz = testVec.getZ();
	//			final float dpx = px - pxLast;
	//			final float dpy = py - pyLast;
	//			final float dpz = pz - pzLast;
	//
	//			pxLast = px;
	//			pyLast = py;
	//			pzLast = pz;
	//			dpxLast = dpx;
	//			dpyLast = dpy;
	//			dpzLast = dpz;
	//
	//			System.out.println(testVec.getX() / worldPerPixel + "   " + testVec.getY() / worldPerPixel + "    " + testVec.getZ() / worldPerPixel);
	//			System.out.println();
	//		}
	//
	//		lastSkylightVector.set(xCurrent, yCurrent, zCurrent);
	//		skyLightVector.set(xCurrent, yCurrent, zCurrent);
	//
	//		computeShadowMatricesInner(camera, radius, bounds);
	//	}

	//	private static void testTexPoint(float x, float y, float z) {
	//		x = Math.round(x / worldPerPixel) * worldPerPixel;
	//		y = Math.round(y / worldPerPixel) * worldPerPixel;
	//		z = Math.round(z / worldPerPixel) * worldPerPixel;
	//
	//		distVec.set(-x, -y, -z);
	//		distVec.cross(skyLightVector);
	//		final float err = distVec.getX() * distVec.getX() + distVec.getY() * distVec.getY() + distVec.getZ() * distVec.getZ();
	//
	//		if (err < bestErr) {
	//			bestErr = err;
	//			xBest = x;
	//			yBest = y;
	//			zBest = z;
	//		}
	//	}

	//	@SuppressWarnings("resource")
	//	private static void computeShadowMatricesWut(Camera camera, float tickDelta, TerrainBounds bounds) {
	//		// We need to keep the skylight projection consistently aligned to
	//		// pixels in the shadowmap texture.  The alignment must be to world
	//		// coordinates in the x/y axis of the skylight perspective.
	//		// Both the frustum center and light position move as the camera moves,
	//		// which causes shimmering if we don't adjust for this movement.
	//
	//		// Because all of our coordinates and matrices at this point are relative to camera,
	//		// we can't test use them for the alignment to world coordinates.
	//		// So we compute the position of the frustum center in world space in a
	//		// projection centered on world origin. Depth doesn't matter here.
	//		// As the camera moves, the x/y distance of its position from the origin
	//		// indicate how much we need to translate the shadowmap projection to maintain alignment.
	//
	//		// Frustum center is at most half view distance away from each frustum plane
	//		// Expanding in each direction by that much should enclose the visible scene
	//		// (Approximate because view frustum isn't a simple box.)
	//		// Note the Y-axis pffset is inverted because MC Y is inverted relative to OpenGL/matrix transform
	//
	//		// To avoid precision issues at the edge of the world, use a world boundary
	//		// that is relatively close - keeping them at regular intervals.
	//
	//		radius = (int) (MinecraftClient.getInstance().gameRenderer.getViewDistance() / 2); //Math.round(straightFrustum.circumRadius());
	//		worldPerPixel = 2f * radius / Pipeline.skyShadowSize;
	//
	//		final int dist = radius * 2;
	//
	//		final float x = skyLightVector.getX() * dist;
	//		final float y = skyLightVector.getY() * dist;
	//		final float z = skyLightVector.getZ() * dist;
	//
	//		final float x0 = (float) (Math.floor(x / worldPerPixel) * worldPerPixel);
	//		final float y0 = (float) (Math.floor(y / worldPerPixel) * worldPerPixel);
	//		final float z0 = (float) (Math.floor(z / worldPerPixel) * worldPerPixel);
	//
	//		final float x1 = x0 + worldPerPixel;
	//		final float y1 = y0 + worldPerPixel;
	//		final float z1 = z0 + worldPerPixel;
	//
	//		bestErr = 10000000f;
	//		testPoint(x0, y0, z0);
	//		testPoint(x0, y0, z1);
	//		testPoint(x0, y1, z0);
	//		testPoint(x0, y1, z1);
	//		testPoint(x1, y0, z0);
	//		testPoint(x1, y0, z1);
	//		testPoint(x1, y1, z0);
	//		testPoint(x1, y1, z1);
	//
	//		final double bestDist = Math.sqrt(xBest * xBest + yBest * yBest + zBest * zBest);
	//
	//		//if (bestErr < 0.05 && Math.abs(bestDist - dist) / worldPerPixel < 0.05) {
	//		final double nx = xBest / bestDist;
	//		final double ny = yBest / bestDist;
	//		final double nz = zBest / bestDist;
	//		final double dot = nx * skyLightVector.getX() + ny * skyLightVector.getY() + nz * skyLightVector.getZ();
	//
	//		if (xCurrent != xBest || yCurrent != yBest || zCurrent != zBest) {
	//			System.out.println(String.format("Err:%f  x:%f  y:%f  z:%f  r:%f   s:%f   dot:%f",
	//					bestErr, xBest / worldPerPixel, yBest / worldPerPixel, zBest / worldPerPixel,
	//					bestDist / worldPerPixel,
	//					xBest / yBest, dot));
	//
	//			xCurrent = xBest;
	//			yCurrent = yBest;
	//			zCurrent = zBest;
	//
	//			shadowViewMatrixExt.lookAt(
	//					xCurrent,
	//					yCurrent,
	//					zCurrent,
	//					0,
	//					0,
	//					0,
	//					0.0f, 0.0f, 1.0f);
	//
	//			testVec.set(radius - worldPerPixel, radius - worldPerPixel, radius - worldPerPixel, 1.0f);
	//			testVec.transform(shadowViewMatrix);
	//
	//			final float px = testVec.getX();
	//			final float py = testVec.getY();
	//			final float pz = testVec.getZ();
	//			final float dpx = px - pxLast;
	//			final float dpy = py - pyLast;
	//			final float dpz = pz - pzLast;
	//
	//			if (Math.signum(dpx) != Math.signum(dpxLast)) {
	//				System.out.println("REVERSE DPX");
	//			}
	//
	//			if (Math.signum(dpy) != Math.signum(dpyLast)) {
	//				System.out.println("REVERSE DPY");
	//			}
	//
	//			if (Math.signum(dpz) != Math.signum(dpzLast)) {
	//				System.out.println("REVERSE DPZ");
	//			}
	//
	//			pxLast = px;
	//			pyLast = py;
	//			pzLast = pz;
	//			dpxLast = dpx;
	//			dpyLast = dpy;
	//			dpzLast = dpz;
	//
	//			System.out.println(testVec);
	//			System.out.println();
	//		}
	//		//}
	//
	//		lastSkylightVector.set(xCurrent, yCurrent, zCurrent);
	//		skyLightVector.set(xCurrent, yCurrent, zCurrent);
	//
	//		computeShadowMatricesInner(camera, radius, bounds);
	//	}

	//private static final Vector3f distVec = new Vector3f();

	//	private static void testPoint(float x, float y, float z) {
	//		x = Math.round(x / worldPerPixel) * worldPerPixel;
	//		y = Math.round(y / worldPerPixel) * worldPerPixel;
	//		z = Math.round(z / worldPerPixel) * worldPerPixel;
	//
	//		distVec.set(-x, -y, -z);
	//		distVec.cross(skyLightVector);
	//		final float err = distVec.getX() * distVec.getX() + distVec.getY() * distVec.getY() + distVec.getZ() * distVec.getZ();
	//
	//		if (err < bestErr) {
	//			bestErr = err;
	//			xBest = x;
	//			yBest = y;
	//			zBest = z;
	//		}
	//	}

	private static void computeShadowMatricesInner(Camera camera, float tickDelta, TerrainBounds bounds) {
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

		//final int radius = (int) (MinecraftClient.getInstance().gameRenderer.getViewDistance() / 2); //Math.round(straightFrustum.circumRadius());
		final int sqRadius = radius * radius;

		// look at world origin to compute relative scale of world XYZ
//		shadowViewMatrixExt.lookAt(
//				xCurrent,
//				yCurrent,
//				zCurrent,
//				0,
//				0,
//				0,
//				0.0f, 0.0f, 1.0f);

		//		testVec.set(radius, 0, 0, 1.0f);
		//		testVec.transform(shadowViewMatrix);
		//		final double xProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
		//
		//		testVec.set(0, radius, 0, 1.0f);
		//		testVec.transform(shadowViewMatrix);
		//		final double yProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
		//
		//		testVec.set(0, 0, radius, 1.0f);
		//		testVec.transform(shadowViewMatrix);
		//		final double zProjectedRadius = Math.sqrt(testVec.getX() * testVec.getX() + testVec.getY() * testVec.getY());
		//
		//		final double xWorldPerPixel = 2.0 * sqRadius / xProjectedRadius / Pipeline.skyShadowSize;
		//		final double yWorldPerPixel = 2.0 * sqRadius / yProjectedRadius / Pipeline.skyShadowSize;
		//		final double zWorldPerPixel = 2.0 * sqRadius / zProjectedRadius / Pipeline.skyShadowSize;
		//
		//		final float mx = radius * WorldDataManager.cameraVector.getX(); //straightFrustum.circumCenterX();
		//		final float my = radius * WorldDataManager.cameraVector.getY(); //straightFrustum.circumCenterY();
		//		final float mz = radius * WorldDataManager.cameraVector.getZ(); //straightFrustum.circumCenterZ();
		//
		//		final double fwx = WorldDataManager.cameraXd + mx;
		//		final double fwy = WorldDataManager.cameraYd + my;
		//		final double fwz = WorldDataManager.cameraZd + mz;
		//
		//		// clamp to pixel boundary
		//		final double cfwx = Math.floor(fwx / xWorldPerPixel) * xWorldPerPixel;
		//		final double cfwy = Math.floor(fwy / yWorldPerPixel) * yWorldPerPixel;
		//		final double cfwz = Math.floor(fwz / zWorldPerPixel) * zWorldPerPixel;
		//
		//		final double fdx = fwx - cfwx;
		//		final double fdy = fwy - cfwy;
		//		final double fdz = fwz - cfwz;
		//
		//		testVec.set((float) fdx, (float) fdy, (float) fdz, 1.0f);
		//		testVec.transform(shadowViewMatrix);

		final float dx = 0; //testVec.getX();
		final float dy = 0; //testVec.getY();

		lastSkyLightPosition.set(skyLightPosition.getX(), skyLightPosition.getY(), skyLightPosition.getZ());

//		skyLightPosition.set(
//				mx + xCurrent,
//				my + yCurrent,
//				mz + zCurrent);

		skyLightPosition.set(
				xCurrent,
				yCurrent,
				zCurrent);

		// Look from skylight towards center of the view frustum in camera space
//		shadowViewMatrixExt.lookAt(
//				WorldDataManager.skyLightPosition.getX(),
//				WorldDataManager.skyLightPosition.getY(),
//				WorldDataManager.skyLightPosition.getZ(),
//				mx, my, mz,
//				0.0f, 0.0f, 1.0f);

		shadowViewMatrixInvExt.set(shadowViewMatrixExt);
		shadowViewMatrixInv.invert();

		bounds.computeViewBounds(shadowViewMatrixExt, WorldDataManager.cameraX, WorldDataManager.cameraY, WorldDataManager.cameraZ);

		// Construct ortho matrix using bounding sphere box computed above.
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
