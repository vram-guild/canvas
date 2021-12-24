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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.renderer.LightTexture;

import grondag.canvas.shader.data.ShaderDataManager;

@Mixin(LightTexture.class)
public abstract class MixinLightTexture {
	@ModifyArg(method = "updateLightTexture", index = 2, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"))
	private int onSetPixelRgba(int i, int j, int color) {
		if (i == 15 && j == 15) {
			ShaderDataManager.updateEmissiveColor(color);
		}

		return color;
	}
}
