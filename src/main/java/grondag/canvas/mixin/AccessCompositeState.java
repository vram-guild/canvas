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

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;

@Mixin(CompositeState.class)
public interface AccessCompositeState {
	@Accessor
	RenderStateShard.EmptyTextureStateShard getTextureState();

	@Accessor
	RenderStateShard.TransparencyStateShard getTransparencyState();

	@Accessor
	RenderStateShard.DepthTestStateShard getDepthTestState();

	@Accessor
	RenderStateShard.CullStateShard getCullState();

	@Accessor
	RenderStateShard.LightmapStateShard getLightmapState();

	@Accessor
	RenderStateShard.OverlayStateShard getOverlayState();

	@Accessor
	RenderStateShard.LayeringStateShard getLayeringState();

	@Accessor
	RenderStateShard.OutputStateShard getOutputState();

	@Accessor
	RenderStateShard.TexturingStateShard getTexturingState();

	@Accessor
	RenderStateShard.WriteMaskStateShard getWriteMaskState();

	@Accessor
	RenderStateShard.LineStateShard getLineState();

	@Accessor
	RenderType.OutlineProperty getOutlineProperty();

	@Accessor
	ImmutableList<RenderStateShard> getStates();

	@Accessor
	RenderStateShard.ShaderStateShard getShaderState();
}
