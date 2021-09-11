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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.texture.NativeImage;

import grondag.canvas.mixinterface.NativeImageExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(NativeImage.class)
public class MixinNativeImage implements NativeImageExt {
	@Shadow private long pointer;

	private CombinedSpriteAnimation combinedAnimation = null;

	@Override
	public void canvas_setCombinedAnimation(CombinedSpriteAnimation combined) {
		this.combinedAnimation = combined;
	}

	@Override
	public long canvas_pointer() {
		return pointer;
	}

	@Inject(at = @At("HEAD"), method = "uploadInternal", cancellable = true)
	private void onUploadInternal(final int level, int toX, int toY, int fromX, int fromY, int width, int height, boolean bl, boolean bl2, boolean bl3, boolean bl4, CallbackInfo ci) {
		if (combinedAnimation != null) {
			combinedAnimation.uploadSubImage((NativeImage) (Object) this, level, toX, toY, fromX, fromY, width, height);
			ci.cancel();
		}
	}
}
