/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;

import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.render.RenderLayerHandler;

@Mixin(targets = "net.minecraft.client.render.RenderLayer$MultiPhase")
abstract class MixinMultiPhase extends RenderLayer implements MultiPhaseExt {
	private MixinMultiPhase(String name, VertexFormat vertexFormat, int drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	@Shadow private Optional<RenderLayer> affectedOutline;
	@Shadow private boolean outline;
	@Shadow private RenderLayer.MultiPhaseParameters phases;

	@Override
	public Optional<RenderLayer> canvas_affectedOutline() {
		return affectedOutline;
	}

	@Override
	public boolean canvas_outline() {
		return outline;
	}

	@Override
	public AccessMultiPhaseParameters canvas_phases() {
		return (AccessMultiPhaseParameters)(Object) phases;
	}

	@Override
	public void canvas_startDrawing() {
		super.startDrawing();
	}

	@Override
	public void canvas_endDrawing() {
		super.endDrawing();
	}

	@Override
	public void startDrawing() {
		super.startDrawing();
		RenderLayerHandler.startDrawing(this);
	}

	@Override
	public void endDrawing() {
		RenderLayerHandler.endDrawing(this);
		super.endDrawing();
	}
}
