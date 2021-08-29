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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;

import grondag.canvas.mixinterface.AnimationResourceMetadataExt;

@Mixin(AnimationResourceMetadata.class)
public class MixinAnimationResourceMetadata implements AnimationResourceMetadataExt {
	@Shadow private List<AnimationFrameResourceMetadata> frames;
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private int defaultFrameTime;
	@Shadow private boolean interpolate;

	private int pngWidth, pngHeight, frameCount;

	@Inject(method = "ensureImageSize", at = @At("HEAD"))
	private void beforeTick(int pngWidth, int pngHeight, CallbackInfoReturnable<Pair<Integer, Integer>> ci) {
		this.pngWidth = pngWidth;
		this.pngHeight = pngHeight;
	}

	@Override
	public boolean canvas_willAnimate(int spriteWidth, int spriteHeight) {
		// forecasts the outcome of Sprite.createAnimation()
		final AnimationResourceMetadata animationResourceMetadata = (AnimationResourceMetadata) (Object) this;
		frameCount = 0;

		animationResourceMetadata.forEachFrame((ix, jx) -> {
			++frameCount;
		});

		if (frameCount == 0) {
			final int widthFrames = pngWidth / animationResourceMetadata.getWidth(spriteWidth);
			final int hieghtFrames = pngHeight / animationResourceMetadata.getHeight(spriteHeight);
			return widthFrames * hieghtFrames > 1;
		} else {
			return frameCount > 1;
		}
	}
}
