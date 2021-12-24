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

package grondag.canvas.texture;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;

import grondag.canvas.mixinterface.NativeImageExt;

public final class CombinedSpriteAnimation implements AutoCloseable {
	private final NativeImage[] images;
	public final int width, height, size;
	private int x0, y0, x1, y1;

	public CombinedSpriteAnimation(TextureAtlas owner, int x0, int y0, int x1, int y1, int lodCount) {
		width = Mth.smallestEncompassingPowerOfTwo(x1 - x0);
		height = Mth.smallestEncompassingPowerOfTwo(y1 - y0);
		size = lodCount + 1;
		images = new NativeImage[size];

		for (int i = 0; i < size; ++i) {
			images[i] = new NativeImage(width >> i, height >> i, false);
		}

		reset();
	}

	@Override
	public void close() {
		for (final var image : images) {
			if (image != null) {
				image.close();
			}
		}
	}

	public void reset() {
		x0 = Integer.MAX_VALUE;
		y0 = Integer.MAX_VALUE;
		x1 = Integer.MIN_VALUE;
		y1 = Integer.MIN_VALUE;
	}

	public void uploadSubImage(final NativeImage source, final int level, final int toX, final int toY, int fromX, int fromY, final int width, final int height) {
		final var target = images[level];
		x0 = Math.min(x0, toX);
		y0 = Math.min(y0, toY);
		x1 = Math.max(x1, toX + width);
		y1 = Math.max(y1, toY + height);
		final long runLength = width * 4L;
		final long sourceBasePtr = ((NativeImageExt) (Object) source).canvas_pointer();
		final long targetBasePtr = ((NativeImageExt) (Object) target).canvas_pointer();

		for (int j = 0; j < height; ++j) {
			final int srcY = j + fromY;
			final long sourceOffset = (fromX + (long) srcY * source.getWidth()) * 4L;

			final int destY = j + toY;
			final long targetOffset = (toX + (long) destY * target.getWidth()) * 4L;

			MemoryUtil.memCopy(sourceBasePtr + sourceOffset, targetBasePtr + targetOffset, runLength);
		}
	}

	public void uploadCombined() {
		if (x0 != Integer.MAX_VALUE) {
			for (int k = 0; k < size; ++k) {
				images[k].upload(k, x0 >> k, y0 >> k, x0 >> k, y0 >> k, (x1 - x0) >> k, (y1 - y0) >> k, size > 1, false);
			}
		}
	}
}
