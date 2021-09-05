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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.texture.TextureStitcher;

import grondag.canvas.config.Configurator;

@Mixin(TextureStitcher.Slot.class)
public class MixinTextureStitcherSlot {
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private List<TextureStitcher.Slot> subSlots;
	@Shadow private TextureStitcher.Holder texture;

	/**
	 * The changes we made for animated sprite order means a slot big enough
	 * to hold the max size texture may not be immediately filled by it when
	 * created.  If the slot is later used to try to fit such a texture, the
	 * vanilla logic doesn't include logic to test for the presence of subslots
	 * and it will consume the entire slot, effectively dissappearing all the
	 * sprites in subslots.
	 */
	@Inject(at = @At("HEAD"), method = "fit", cancellable = true)
	private void onFit(TextureStitcher.Holder holder, CallbackInfoReturnable<Boolean> ci) {
		if (Configurator.groupAnimatedSprites && this.texture == null && this.subSlots != null && holder.width == width && holder.height == height) {
			ci.setReturnValue(false);
		}
	}
}
