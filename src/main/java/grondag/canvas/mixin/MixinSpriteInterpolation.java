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

package grondag.canvas.mixin;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.mixinterface.AnimatedTextureExt;
import grondag.canvas.mixinterface.NativeImageExt;
import grondag.canvas.mixinterface.SpriteExt;

@Mixin(TextureAtlasSprite.InterpolationData.class)
public class MixinSpriteInterpolation {
	@Shadow private NativeImage[] activeFrame;

	@Shadow(aliases = {"this$0", "a", "field_21757"})
	private TextureAtlasSprite parent;

	/**
	 * Hat tip to JellySquid for the approach used here.
	 *
	 * @author Grondag
	 * @reason Vanilla code is too slow
	 */
	@Overwrite
	public void uploadInterpolatedFrame(TextureAtlasSprite.AnimatedTexture animation) {
		final var animationExt = (AnimatedTextureExt) animation;
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

		for (int layer = 0; layer < this.activeFrame.length; layer++) {
			final int width = parent.getWidth() >> layer;
			final int height = parent.getHeight() >> layer;

			final int x0 = ((curIndex % frameCount) * width);
			final int y0 = ((curIndex / frameCount) * height);

			final int x1 = ((nextIndex % frameCount) * width);
			final int y1 = ((nextIndex / frameCount) * height);

			final var sourceImage = parentExt.canvas_images()[layer];
			final int imageWidth = sourceImage.getWidth();
			final long sourceAddress = ((NativeImageExt) (Object) sourceImage).canvas_pointer();
			final var targetImage = (NativeImageExt) (Object) activeFrame[layer];
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

		parentExt.canvas_upload(0, 0, this.activeFrame);
	}
}
