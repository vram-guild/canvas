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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
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
	@Shadow @Final List<TextureAtlasSprite.FrameInfo> frames;
	@Shadow @Final private int frameRowSize;

	@Nullable
	@Shadow @Final private TextureAtlasSprite.InterpolationData interpolationData;

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
