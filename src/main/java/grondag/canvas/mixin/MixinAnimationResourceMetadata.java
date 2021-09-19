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

import java.util.List;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

import grondag.canvas.mixinterface.AnimationResourceMetadataExt;
import grondag.canvas.mixinterface.SimulatedFrame;

@Mixin(AnimationMetadataSection.class)
public class MixinAnimationResourceMetadata implements AnimationResourceMetadataExt {
	@Shadow private List<AnimationFrame> frames;
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private int defaultFrameTime;
	@Shadow private boolean interpolate;

	private int pngWidth, pngHeight;

	@Inject(method = "ensureImageSize", at = @At("HEAD"))
	private void beforeTick(int pngWidth, int pngHeight, CallbackInfoReturnable<Pair<Integer, Integer>> ci) {
		this.pngWidth = pngWidth;
		this.pngHeight = pngHeight;
	}

	@Override
	public boolean canvas_willAnimate(int spriteWidth, int spriteHeight) {
		// forecasts the outcome of Sprite.createAnimation()

		final AnimationMetadataSection animationResourceMetadata = (AnimationMetadataSection) (Object) this;
		final int widthFrames = pngWidth / animationResourceMetadata.getFrameWidth(spriteWidth);
		final int heightFrames = pngHeight / animationResourceMetadata.getFrameHeight(spriteHeight);
		final int expectedFrames = widthFrames * heightFrames;
		final var frames = new ObjectArrayList<SimulatedFrame>();

		animationResourceMetadata.forEachFrame((index, time) -> {
			frames.add(new SimulatedFrame(index, time));
		});

		if (frames.isEmpty()) {
			for (int p = 0; p < expectedFrames; ++p) {
				frames.add(new SimulatedFrame(p, animationResourceMetadata.getDefaultFrameTime()));
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
