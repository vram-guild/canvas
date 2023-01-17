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

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import grondag.canvas.mixinterface.AnimationMetadataSectionExt;
import grondag.canvas.mixinterface.SimulatedFrame;

@Mixin(AnimationMetadataSection.class)
public class MixinAnimationMetadataSection implements AnimationMetadataSectionExt {
	@Shadow private int frameWidth;
	@Shadow private int frameHeight;

	private int pngWidth, pngHeight;

	@Inject(method = "calculateFrameSize", at = @At("HEAD"))
	private void beforeGetFrameSize(int pngWidth, int pngHeight, CallbackInfoReturnable<Pair<Integer, Integer>> ci) {
		this.pngWidth = pngWidth;
		this.pngHeight = pngHeight;
	}

	public int getFrameHeight(int defHeight) {
		return frameHeight == -1 ? defHeight : frameHeight;
	}

	public int getFrameWidth(int defWidth) {
		return frameWidth == -1 ? defWidth : frameWidth;
	}

	@Override
	public boolean canvas_willAnimate(int spriteWidth, int spriteHeight) {
		// forecasts the outcome of Sprite.createAnimation()

		final AnimationMetadataSection animationMetadata = (AnimationMetadataSection) (Object) this;
		final int widthFrames = pngWidth / getFrameWidth(spriteWidth);
		final int heightFrames = pngHeight / getFrameHeight(spriteHeight);
		final int expectedFrames = widthFrames * heightFrames;
		final var frames = new ObjectArrayList<SimulatedFrame>();

		animationMetadata.forEachFrame((index, time) -> {
			frames.add(new SimulatedFrame(index, time));
		});

		if (frames.isEmpty()) {
			for (int p = 0; p < expectedFrames; ++p) {
				frames.add(new SimulatedFrame(p, animationMetadata.getDefaultFrameTime()));
			}
		} else {
			for (final var iterator = frames.iterator(); iterator.hasNext(); ) {
				final var animationFrame = iterator.next();

				if (animationFrame.time() <= 0 || animationFrame.index() < 0 || animationFrame.index() >= expectedFrames) {
					iterator.remove();
				}
			}
		}

		return frames.size() > 1;
	}
}
