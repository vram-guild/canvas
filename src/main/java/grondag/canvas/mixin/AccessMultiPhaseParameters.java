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

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase;

@Mixin(MultiPhaseParameters.class)
public interface AccessMultiPhaseParameters {
	@Accessor
	RenderPhase.class_5939 getTexture();

	@Accessor
	RenderPhase.Transparency getTransparency();

	@Accessor
	RenderPhase.DepthTest getDepthTest();

	@Accessor
	RenderPhase.Cull getCull();

	@Accessor
	RenderPhase.Lightmap getLightmap();

	@Accessor
	RenderPhase.Overlay getOverlay();

	@Accessor
	RenderPhase.Layering getLayering();

	@Accessor
	RenderPhase.Target getTarget();

	@Accessor
	RenderPhase.Texturing getTexturing();

	@Accessor
	RenderPhase.WriteMaskState getWriteMaskState();

	@Accessor
	RenderPhase.LineWidth getLineWidth();

	@Accessor
	RenderLayer.OutlineMode getOutlineMode();

	@Accessor
	ImmutableList<RenderPhase> getPhases();

	@Accessor
	RenderLayer.class_5942 getField_29461();
}
