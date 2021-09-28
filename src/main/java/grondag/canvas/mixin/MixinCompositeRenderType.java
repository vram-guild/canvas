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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.CompositeRenderTypeExt;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
abstract class MixinCompositeRenderType extends RenderType implements CompositeRenderTypeExt {
	private @Nullable RenderMaterialImpl materialState;

	private MixinCompositeRenderType(String name, VertexFormat vertexFormat, VertexFormat.Mode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	@Override
	public RenderMaterialImpl canvas_materialState() {
		RenderMaterialImpl result = materialState;

		if (result == null) {
			result = (RenderMaterialImpl) RenderTypeUtil.toMaterial(this);
			materialState = result;
		}

		return result;
	}
}
