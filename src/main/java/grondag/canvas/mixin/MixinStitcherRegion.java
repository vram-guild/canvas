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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.texture.Stitcher;

import grondag.canvas.config.Configurator;

@Mixin(Stitcher.Region.class)
public class MixinStitcherRegion {
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private List<Stitcher.Region> subSlots;
	@Shadow private Stitcher.Holder holder;

	/**
	 * The changes we made for animated sprite order means a slot big enough
	 * to hold the max size texture may not be immediately filled by it when
	 * created.  If the slot is later used to try to fit such a texture, the
	 * vanilla logic doesn't include logic to test for the presence of subslots
	 * and it will consume the entire slot, effectively dissappearing all the
	 * sprites in subslots.
	 */
	@Inject(at = @At("HEAD"), method = "add", cancellable = true)
	private void onAdd(Stitcher.Holder holder, CallbackInfoReturnable<Boolean> ci) {
		// NP: the holder==null check is included because vanilla logic will already return false
		// if holder is non-null, so there's no need for us to override the result in that case.
		if (Configurator.groupAnimatedSprites && this.holder == null && this.subSlots != null && holder.width == width && holder.height == height) {
			ci.setReturnValue(false);
		}
	}
}
