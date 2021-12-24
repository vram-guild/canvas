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

package grondag.canvas.terrain.occlusion.base;

import static grondag.bitraster.Constants.PIXEL_HEIGHT;
import static grondag.bitraster.Constants.PIXEL_WIDTH;

import java.io.File;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import io.vram.frex.api.math.FastMatrix4f;

import grondag.bitraster.AbstractRasterizer;
import grondag.bitraster.BoxOccluder;
import grondag.bitraster.Matrix4L;
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

	protected static void copyMatrixF2L(FastMatrix4f src, Matrix4L dst) {
		dst.set(
				src.f_m00(), src.f_m10(), src.f_m20(), src.f_m30(),
				src.f_m01(), src.f_m11(), src.f_m21(), src.f_m31(),
				src.f_m02(), src.f_m12(), src.f_m22(), src.f_m32(),
				src.f_m03(), src.f_m13(), src.f_m23(), src.f_m33());
	}
}
