/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.material.state.wip.CanvasRenderMaterial;
import grondag.canvas.mixinterface.CompositeRenderTypeExt;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
abstract class MixinCompositeRenderType extends RenderType implements CompositeRenderTypeExt {
	private @Nullable CanvasRenderMaterial materialState;

	private MixinCompositeRenderType(String name, VertexFormat vertexFormat, VertexFormat.Mode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	@Override
	public CanvasRenderMaterial canvas_materialState() {
		CanvasRenderMaterial result = materialState;

		if (result == null) {
			result = (CanvasRenderMaterial) RenderTypeUtil.toMaterial(this);
			materialState = result;
		}

		return result;
	}
}
