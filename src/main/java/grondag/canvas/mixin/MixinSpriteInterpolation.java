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

package grondag.canvas.mixin;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

import grondag.canvas.mixinterface.CombinedAnimationConsumer;
import grondag.canvas.mixinterface.NativeImageExt;
import grondag.canvas.mixinterface.SpriteAnimationExt;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(Sprite.Interpolation.class)
public class MixinSpriteInterpolation implements CombinedAnimationConsumer {
	@Shadow private NativeImage[] images;

	@Shadow(aliases = "field_21757")
	private Sprite parent;

	/**
	 * Hat tip to JellySquid for the approach used here.
	 *
	 * @author Grondag
	 * @reason Vanilla code is too slow
	 */
	@Overwrite
	public void apply(Sprite.Animation animation) {
		final var animationExt = (SpriteAnimationExt) animation;
		final int frameIndex = animationExt.canvas_frameIndex();
		final var frames = animationExt.canvas_frames();
		final var currentFrame = frames.get(frameIndex);
		final int curIndex = currentFrame.index;
		final int nextIndex = frames.get((frameIndex + 1) % frames.size()).index;

		if (curIndex == nextIndex) {
			return;
		}

		final int frameCount = animationExt.canvas_frameCount();
		final var parentExt = (SpriteExt) parent;

		final float dt = 1.0F - (float) animationExt.canvas_frameTicks() / (float) currentFrame.time;

		final int w0 = (int) (256 * dt);
		final int w1 = 256 - w0;

		for (int layer = 0; layer < this.images.length; layer++) {
			final int width = parent.getWidth() >> layer;
			final int height = parent.getHeight() >> layer;

			final int x0 = ((curIndex % frameCount) * width);
			final int y0 = ((curIndex / frameCount) * height);

			final int x1 = ((nextIndex % frameCount) * width);
			final int y1 = ((nextIndex / frameCount) * height);

			final var sourceImage = parentExt.canvas_images()[layer];
			final int imageWidth = sourceImage.getWidth();
			final long sourceAddress = ((NativeImageExt) (Object) sourceImage).canvas_pointer();
			final var targetImage = (NativeImageExt) (Object) images[layer];
			long targetAddress = targetImage.canvas_pointer();

			long srcAddr0 = sourceAddress + (x0 + ((long) y0 * imageWidth) * 4);
			long srcAddr1 = sourceAddress + (x1 + ((long) y1 * imageWidth) * 4);
			final int pixelCount = width * height;

			for (int i = 0; i < pixelCount; i++) {
				long c0 = Integer.toUnsignedLong(MemoryUtil.memGetInt(srcAddr0));
				long c1 = Integer.toUnsignedLong(MemoryUtil.memGetInt(srcAddr1));

				c0 = (c0 & 0xFF00FFL) | ((c0 & 0xFF00FF00L) << 24);
				c1 = (c1 & 0xFF00FFL) | ((c1 & 0xFF00FF00L) << 24);

				final long cBlended = (c0 * w0 + c1 * w1 + 0x007F007F007F007FL) >>> 8;
				final int result = (int) (cBlended & 0xFF00FFL | ((cBlended >>> 24) & 0xFF00FF00L));

				MemoryUtil.memPutInt(targetAddress, result);

				srcAddr0 += 4;
				srcAddr1 += 4;
				targetAddress += 4;
			}
		}

		parentExt.canvas_upload(0, 0, this.images);
	}

	@Override
	public void canvas_setCombinedAnimation(CombinedSpriteAnimation combined) {
		for (final var img : images) {
			((CombinedAnimationConsumer) (Object) img).canvas_setCombinedAnimation(combined);
		}
	}
}
