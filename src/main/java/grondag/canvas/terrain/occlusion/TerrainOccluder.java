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

import static grondag.bitraster.Constants.PIXEL_HEIGHT;
import static grondag.bitraster.Constants.PIXEL_WIDTH;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.bitraster.BoxOccluder;
import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.terrain.region.BuiltRenderRegion;

public class TerrainOccluder extends BoxOccluder {
	/**
	 * This frustum is a snapshot of the view frustum and may lag behind for a frame or two.
	 * A snapshot is used because occlusion test can happen off the main render thread and we
	 * need a stable frustum for each occlusion update.
	 */
	private final TerrainFrustum occlusionFrustum = new TerrainFrustum();

	private long nextRasterOutputTime;

	/**
	 * Synchronizes our frustum snapshot with the input, typically the active terrain view frustum.
	 * Should be called from the main thread when the source is known to be stable and correct.
	 * The snapshot will be used (potentially off thread) for all occlusion tests until the next update.
	 */
	public void updateFrustum(TerrainFrustum source) {
		occlusionFrustum.copy(source);
	}

	public int frustumViewVersion() {
		return occlusionFrustum.viewVersion();
	}

	public int frustumPositionVersion() {
		return occlusionFrustum.occlusionPositionVersion();
	}

	public Vec3d frustumCameraPos() {
		return occlusionFrustum.lastCameraPos();
	}

	public boolean isRegionVisible(BuiltRenderRegion builtRenderRegion) {
		return occlusionFrustum.isRegionVisible(builtRenderRegion);
	}

	public void prepareRegion(BlockPos origin, int occlusionRange, int squaredChunkDistance) {
		super.prepareRegion(origin.getX(), origin.getY(), origin.getZ(), occlusionRange, squaredChunkDistance);
	}

	public void outputRaster() {
		outputRaster("canvas_occlusion_raster.png", false);
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
		final int viewVersion = occlusionFrustum.viewVersion();
		final Vec3d cameraPos = occlusionFrustum.lastCameraPos();
		final Matrix4fExt projectionMatrix = occlusionFrustum.projectionMatrix();
		final Matrix4fExt modelMatrix = occlusionFrustum.modelMatrix();
		return super.prepareScene(viewVersion, cameraPos.x, cameraPos.y, cameraPos.z, modelMatrix::copyTo, projectionMatrix::copyTo);
	}

	public boolean isEmptyRegionVisible(BlockPos origin) {
		return super.isEmptyRegionVisible(origin.getX(), origin.getY(), origin.getZ());
	}
}
