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

package grondag.canvas.render;

import grondag.canvas.config.Configurator;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.pipeline.PipelineFramebuffer;

@FunctionalInterface
interface WorldRenderPass {
	void render(WorldRenderPassContext context);

	static WorldRenderPass copyDepth(PipelineFramebuffer source, PipelineFramebuffer dest) {
		return ctx -> source.copyDepthFrom(dest);
	}

	static WorldRenderPass profilerSwap(String token) {
		return ctx -> {
			Configurator.lagFinder.swap(token);
			ctx.profiler.swap(token);
		};
	}

	static WorldRenderPass materialFog(boolean enable) {
		return ctx -> MaterialFog.allow(enable);
	}

	static WorldRenderPass lightUpdates() {
		return ctx -> ctx.mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
	}
}
