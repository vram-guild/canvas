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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldRenderPasses implements WorldRenderPass {
	private final ObjectArrayList<WorldRenderPass> passes = new ObjectArrayList<>();

	public WorldRenderPasses() {
		passes.add(WorldRenderPass.materialFog(true));
		passes.add(WorldRenderPass.profilerSwap("clear"));
		passes.add(WorldRenderPass.bindFramebuffer("default"));
		passes.add(WorldRenderPass.setVanillaClearColor());
		passes.add(WorldRenderPass.clearColorDepth());

		passes.add(WorldRenderPass.conditional(
			ctx -> ctx.mc.options.viewDistance >= 4,
			WorldRenderPass.chain(
				WorldRenderPass.profilerSwap("sky"),
				WorldRenderPass.setupVanillaSkyFog(),
				WorldRenderPass.renderVanillaSky()
			)
		));

		passes.add(WorldRenderPass.profilerSwap("fog"));
		passes.add(WorldRenderPass.setupVanillaTerrainFog());
	}

	@Override
	public void render(WorldRenderPassContext context) {
		final int limit = passes.size();

		for (int i = 0; i < limit; ++i) {
			passes.get(i).render(context);
		}
	}

	public static WorldRenderPasses current() {
		return new WorldRenderPasses();
	}
}
