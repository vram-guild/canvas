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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.mixinterface.AnimatedTextureExt;
import grondag.canvas.mixinterface.CombinedAnimationConsumer;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(TextureAtlasSprite.AnimatedTexture.class)
public class MixinAnimatedTexture implements AnimatedTextureExt {
	@Shadow(aliases = {"this$0", "a", "field_28469"})
	@Dynamic private TextureAtlasSprite parent;

	@Shadow int frame;
	@Shadow int subFrame;
	@Shadow List<TextureAtlasSprite.FrameInfo> frames;
	@Shadow private int frameRowSize;

	@Nullable
	@Shadow private TextureAtlasSprite.InterpolationData interpolationData;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void beforeTick(CallbackInfo ci) {
		if (!((SpriteExt) parent).canvas_shouldAnimate()) {
			ci.cancel();
		}
	}

	@Override
	public TextureAtlasSprite.InterpolationData canvas_interpolation() {
		return interpolationData;
	}

	@Override
	public int canvas_frameCount() {
		return frameRowSize;
	}

	@Override
	public int canvas_frameIndex() {
		return frame;
	}

	@Override
	public int canvas_frameTicks() {
		return subFrame;
	}

	@Override
	public List<TextureAtlasSprite.FrameInfo> canvas_frames() {
		return frames;
	}

	@Override
	public void canvas_setCombinedAnimation(CombinedSpriteAnimation combined) {
		if (interpolationData != null) {
			((CombinedAnimationConsumer) (Object) interpolationData).canvas_setCombinedAnimation(combined);
		}
	}
}
