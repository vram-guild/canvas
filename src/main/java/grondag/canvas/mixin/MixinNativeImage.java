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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.NativeImage;

import grondag.canvas.mixinterface.NativeImageExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(NativeImage.class)
public class MixinNativeImage implements NativeImageExt {
	@Shadow private long pixels;

	private CombinedSpriteAnimation combinedAnimation = null;

	@Override
	public void canvas_setCombinedAnimation(CombinedSpriteAnimation combined) {
		this.combinedAnimation = combined;
	}

	@Override
	public long canvas_pointer() {
		return pixels;
	}

	@Inject(at = @At("HEAD"), method = "_upload", cancellable = true)
	private void onUploadInternal(final int level, int toX, int toY, int fromX, int fromY, int width, int height, boolean bl, boolean bl2, boolean bl3, boolean bl4, CallbackInfo ci) {
		if (combinedAnimation != null) {
			combinedAnimation.uploadSubImage((NativeImage) (Object) this, level, toX, toY, fromX, fromY, width, height);
			ci.cancel();
		}
	}
}
