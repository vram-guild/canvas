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

package grondag.canvas.terrain.occlusion.base;

import static grondag.bitraster.Constants.PIXEL_HEIGHT;
import static grondag.bitraster.Constants.PIXEL_WIDTH;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import grondag.bitraster.AbstractRasterizer;
import grondag.bitraster.BoxOccluder;
import grondag.canvas.CanvasMod;
import grondag.canvas.terrain.region.RegionPosition;

/**
 * Manages software rasterizer instance used for occlusion testing.
 */
public abstract class AbstractOccluder extends BoxOccluder {
	/** Timer for debug output. */
	private long nextRasterOutputTime;

	/** File name for debug output. */
	private final String rasterName;

	public AbstractOccluder(AbstractRasterizer raster, String rasterName) {
		super(raster);
		this.rasterName = rasterName;
	}

	public void outputRaster() {
		final long t = System.currentTimeMillis();

		if (t >= nextRasterOutputTime) {
			nextRasterOutputTime = t + 1000;
			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelRGBA(x, y, raster.isPixelClear(x, y) ? -1 : 0xFF000000);
				}
			}

			nativeImage.flipY();

			@SuppressWarnings("resource") final File file = new File(Minecraft.getInstance().gameDirectory, rasterName);

			Util.ioPool().execute(() -> {
				try {
					nativeImage.writeToFile(file);
				} catch (final Exception e) {
					CanvasMod.LOG.warn("Couldn't save occluder image", e);
				} finally {
					nativeImage.close();
				}
			});
		}
	}

	/**
	 * Convenience for {@link #prepareRegion(RegionPosition)} followed
	 * by {@link #isBoxVisible(int)} with full box. Use when known this
	 * will be the only test. Otherwise more efficient to call {@link #prepareRegion(RegionPosition)} 1X.
	 */
	public final boolean isEmptyRegionVisible(BlockPos origin, int fuzz) {
		return super.isEmptyRegionVisible(origin.getX(), origin.getY(), origin.getZ(), fuzz);
	}

	/**
	 * Check if needs redrawn and prepares for redraw if so.
	 * When false, regions should not be drawn if they were drawn
	 * after the last positive result.
	 */
	public abstract boolean prepareScene();

	/**
	 * Call before running occlusion tests or draws.
	 * Separated to avoid overhead of multiple initialization.
	 */
	public abstract void prepareRegion(RegionPosition origin);
}
