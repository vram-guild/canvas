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

package grondag.canvas.terrain.occlusion;

import static grondag.bitraster.Constants.DOWN;
import static grondag.bitraster.Constants.EAST;
import static grondag.bitraster.Constants.NORTH;
import static grondag.bitraster.Constants.PIXEL_HEIGHT;
import static grondag.bitraster.Constants.PIXEL_WIDTH;
import static grondag.bitraster.Constants.SOUTH;
import static grondag.bitraster.Constants.UP;
import static grondag.bitraster.Constants.WEST;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;

import grondag.bitraster.BoxOccluder;
import grondag.bitraster.OrthoRasterizer;
import grondag.bitraster.PackedBox;
import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.region.RegionPosition;

public class ShadowOccluder extends BoxOccluder {
	private final Matrix4f shadowViewMatrix = new Matrix4f();
	private final Matrix4fExt shadowViewMatrixExt = (Matrix4fExt) (Object) shadowViewMatrix;

	public final Matrix4f shadowProjMatrix = new Matrix4f();
	private final Matrix4fExt shadowProjMatrixExt = (Matrix4fExt) (Object) shadowProjMatrix;

	private float maxRegionExtent;
	private float r0, x0, y0, r1, x1, y1, r2, x2, y2, r3, x3, y3;
	private int lastViewVersion;
	private Vec3d lastCameraPos;
	private grondag.bitraster.BoxOccluder.BoxTest test;
	private grondag.bitraster.BoxOccluder.BoxDraw draw;

	private long nextRasterOutputTime;

	public ShadowOccluder() {
		super(new OrthoRasterizer());
	}

	public void copyState(TerrainFrustum occlusionFrustum) {
		shadowViewMatrixExt.set(ShadowMatrixData.shadowViewMatrix);
		shadowProjMatrixExt.set(ShadowMatrixData.maxCascadeProjMatrix());
		maxRegionExtent = ShadowMatrixData.regionMaxExtent();
		final float[] cascadeCentersAndRadii = ShadowMatrixData.cascadeCentersAndRadii;
		x0 = cascadeCentersAndRadii[0];
		y0 = cascadeCentersAndRadii[1];
		r0 = cascadeCentersAndRadii[3];

		x1 = cascadeCentersAndRadii[4];
		y1 = cascadeCentersAndRadii[5];
		r1 = cascadeCentersAndRadii[7];

		x2 = cascadeCentersAndRadii[8];
		y2 = cascadeCentersAndRadii[9];
		r2 = cascadeCentersAndRadii[11];

		x3 = cascadeCentersAndRadii[12];
		y3 = cascadeCentersAndRadii[13];
		r3 = cascadeCentersAndRadii[15];

		lastCameraPos = occlusionFrustum.lastCameraPos();
		lastViewVersion = occlusionFrustum.viewVersion();
	}

	public void prepareRegion(RegionPosition origin) {
		// WIP: NOT RANGE_NEAR
		super.prepareRegion(origin.getX(), origin.getY(), origin.getZ(), PackedBox.RANGE_NEAR, origin.shadowDistanceRank());
	}

	public void outputRaster() {
		outputRaster("canvas_shadow_occlusion_raster.png", false);
	}

	public void outputRaster(String fileName, boolean force) {
		final long t = System.currentTimeMillis();

		if (!force && t >= nextRasterOutputTime) {
			force = true;
			nextRasterOutputTime = t + 1000;
		}

		if (force) {
			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelColor(x, y, raster.testPixel(x, y) ? -1 : 0xFF000000);
				}
			}

			nativeImage.mirrorVertically();

			@SuppressWarnings("resource") final File file = new File(MinecraftClient.getInstance().runDirectory, fileName);

			Util.getIoWorkerExecutor().execute(() -> {
				try {
					nativeImage.writeFile(file);
				} catch (final Exception e) {
					CanvasMod.LOG.warn("Couldn't save occluder image", e);
				} finally {
					nativeImage.close();
				}
			});
		}
	}

	/**
	 * Check if needs redrawn and prep for redraw if so.
	 * When false, regions should be drawn only if their occluder version is not current.
	 */
	public boolean prepareScene() {
		return super.prepareScene(lastViewVersion, lastCameraPos.x, lastCameraPos.y, lastCameraPos.z, shadowViewMatrixExt::copyTo, shadowProjMatrixExt::copyTo);
	}

	public boolean isEmptyRegionVisible(BlockPos origin) {
		return super.isEmptyRegionVisible(origin.getX(), origin.getY(), origin.getZ());
	}

	public int cascadeFlags(RegionPosition regionPosition) {
		// Compute center position in light space
		final Vector4f lightSpaceRegionCenter = new Vector4f();
		lightSpaceRegionCenter.set(regionPosition.cameraRelativeCenterX(), regionPosition.cameraRelativeCenterY(), regionPosition.cameraRelativeCenterZ(), 1.0f);
		lightSpaceRegionCenter.transform(ShadowMatrixData.shadowViewMatrix);

		final float centerX = lightSpaceRegionCenter.getX();
		final float centerY = lightSpaceRegionCenter.getY();
		final float extent = maxRegionExtent;
		int result = 0;

		// <= extent = at least partially in
		// < -extent = fully in
		// > extent not in

		final float dx0 = Math.abs(centerX - x0) - r0;
		final float dy0 = Math.abs(centerY - y0) - r0;

		if (dx0 < extent && dy0 < extent) {
			// within shadow projection

			final float dx1 = Math.abs(centerX - x1) - r1;
			final float dy1 = Math.abs(centerY - y1) - r1;

			if (dx1 > -extent || dy1 > -extent) {
				// not fully within 1, so must be in 0
				result |= ShadowMatrixData.CASCADE_FLAG_0;

				if (dx1 > extent || dy1 > extent) {
					// not in 1 at all, only 0
					return result;
				}
			}

			final float dx2 = Math.abs(centerX - x2) - r2;
			final float dy2 = Math.abs(centerY - y2) - r2;

			if (dx2 > -extent || dy2 > -extent) {
				// not fully within 2, so must be in 1
				result |= ShadowMatrixData.CASCADE_FLAG_1;

				if (dx2 > extent || dy2 > extent) {
					// not in 2 at all
					return result;
				}
			}

			final float dx3 = Math.abs(centerX - x3) - r3;
			final float dy3 = Math.abs(centerY - y3) - r3;

			if (dx3 > -extent || dy3 > -extent) {
				// not fully within 3, so must be in 2
				result |= ShadowMatrixData.CASCADE_FLAG_2;

				if (dx3 > extent || dy3 > extent) {
					// not in 3 at all
					return result;
				}
			}

			// If get to here, must be in 3
			result |= ShadowMatrixData.CASCADE_FLAG_3;
		}

		return result;
	}

	public float maxRegionExtent() {
		return maxRegionExtent;
	}

	@Override
	public boolean isBoxVisible(int packedBox) {
		final int x0 = PackedBox.x0(packedBox) - 1;
		final int y0 = PackedBox.y0(packedBox) - 1;
		final int z0 = PackedBox.z0(packedBox) - 1;
		final int x1 = PackedBox.x1(packedBox) + 1;
		final int y1 = PackedBox.y1(packedBox) + 1;
		final int z1 = PackedBox.z1(packedBox) + 1;

		return test.apply(x0, y0, z0, x1, y1, z1);
	}

	@Override
	protected void occludeInner(int packedBox) {
		final int x0 = PackedBox.x0(packedBox);
		final int y0 = PackedBox.y0(packedBox);
		final int z0 = PackedBox.z0(packedBox);
		final int x1 = PackedBox.x1(packedBox);
		final int y1 = PackedBox.y1(packedBox);
		final int z1 = PackedBox.z1(packedBox);

		draw.apply(x0, y0, z0, x1, y1, z1);
	}

	public void setLightVector(Vec3f skylightVector) {
		int outcome = 0;

		if (!MathHelper.approximatelyEquals(skylightVector.getX(), 0)) {
			outcome |= skylightVector.getX() > 0 ? EAST : WEST;
		}

		if (!MathHelper.approximatelyEquals(skylightVector.getZ(), 0)) {
			outcome |= skylightVector.getZ() > 0 ? SOUTH : NORTH;
		}

		if (!MathHelper.approximatelyEquals(skylightVector.getY(), 0)) {
			outcome |= skylightVector.getY() > 0 ? UP : DOWN;
		}

		test = boxTests[outcome];
		draw = boxDraws[outcome];
	}
}

