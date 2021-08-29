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

package grondag.canvas.texture;

import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.mixinterface.NativeImageExt;

@Environment(EnvType.CLIENT)
public final class CombinedSpriteAnimation implements AutoCloseable {
	private final NativeImage[] images;
	private final int w, h, size;
	private int x0, y0, x1, y1;

	public CombinedSpriteAnimation(SpriteAtlasTexture owner, int x0, int y0, int x1, int y1, int lodCount) {
		w = MathHelper.smallestEncompassingPowerOfTwo(x1- x0);
		h = MathHelper.smallestEncompassingPowerOfTwo(y1- y0);
		size = lodCount + 1;
		images = new NativeImage[size];

		for (int i = 0; i < size; ++i) {
			images[i] = new NativeImage(w >> i, h >> i, false);
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
