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

import java.util.Optional;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.vram.frex.mixin.core.AccessCompositeState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.renderer.RenderType;

import grondag.canvas.material.state.RenderTypeHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.CompositeRenderTypeExt;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
abstract class MixinCompositeRenderType extends RenderType implements CompositeRenderTypeExt {
	@Shadow private Optional<RenderType> outline;
	@Shadow private boolean isOutline;
	@Shadow private RenderType.CompositeState state;

	private @Nullable RenderMaterialImpl materialState;

	private MixinCompositeRenderType(String name, VertexFormat vertexFormat, VertexFormat.Mode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	@Override
	public Optional<RenderType> canvas_affectedOutline() {
		return outline;
	}

	@Override
	public String canvas_name() {
		return name;
	}

	@Override
	public boolean canvas_outline() {
		return isOutline;
	}

	@Override
	public AccessCompositeState canvas_phases() {
		return (AccessCompositeState) (Object) state;
	}

	@Override
	public RenderMaterialImpl canvas_materialState() {
		RenderMaterialImpl result = materialState;

		if (result == null) {
			result = (RenderMaterialImpl) RenderTypeHelper.copyFromRenderType(this);
			materialState = result;
		}

		return result;
	}

	@Override
	public void canvas_startDrawing() {
		super.setupRenderState();
	}

	@Override
	public void canvas_endDrawing() {
		super.clearRenderState();
	}
}
