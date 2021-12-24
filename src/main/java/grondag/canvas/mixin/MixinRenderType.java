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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.RenderType;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.mixinterface.RenderTypeExt;

@Mixin(RenderType.class)
abstract class MixinRenderType implements RenderTypeExt {
	private @Nullable CanvasRenderMaterial materialState;

	@Override
	public CanvasRenderMaterial canvas_materialState() {
		CanvasRenderMaterial result = materialState;

		if (result == null) {
			result = (CanvasRenderMaterial) RenderTypeUtil.toMaterial((RenderType) (Object) this);
			materialState = result;
		}

		return result;
	}
}
