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

import net.minecraft.client.texture.Sprite;

import grondag.canvas.mixinterface.SpriteAnimationExt;
import grondag.canvas.mixinterface.SpriteExt;

@Mixin(Sprite.Animation.class)
public class MixinSpriteAnimation implements SpriteAnimationExt {
	@Shadow(aliases = "field_28469")
	@Dynamic private Sprite parent;

	@Shadow private int frameCount;
	@Shadow int frameIndex;
	@Shadow int frameTicks;
	@Shadow List<Sprite.AnimationFrame> frames;

	@Nullable private Sprite.Interpolation interpolation;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void beforeTick(CallbackInfo ci) {
		if (!((SpriteExt) parent).canvas_shouldAnimate()) {
			ci.cancel();
		}
	}

	@Override
	public Sprite.Interpolation canvas_interpolation() {
		return interpolation;
	}

	@Override
	public int canvas_frameCount() {
		return frameCount;
	}

	@Override
	public int canvas_frameIndex() {
		return frameIndex;
	}

	@Override
	public int canvas_frameTicks() {
		return frameTicks;
	}

	@Override
	public List<Sprite.AnimationFrame> canvas_frames() {
		return frames;
	}
}
