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

import grondag.canvas.buffer.encoding.DrawableBuffer;

public class WorldRenderPasses {
	private final ObjectArrayList<WorldRenderPass> passes = new ObjectArrayList<>();
	private final WorldRenderPassContext context = new WorldRenderPassContext();

	public WorldRenderPasses() {
		//WIP
	}

	public void render(CanvasWorldRenderer canvasWorldRenderer, double cameraX, double cameraY, double cameraZ, DrawableBuffer entityBuffer) {
		context.canvasWorldRenderer = canvasWorldRenderer;
		context.cameraX = cameraX;
		context.cameraY = cameraY;
		context.cameraZ = cameraZ;
		context.entityBuffer = entityBuffer;

		final int limit = passes.size();

		for (int i = 0; i < limit; ++i) {
			passes.get(i).render(context);
		}
	}

	public static WorldRenderPasses current() {
		return new WorldRenderPasses();
	}
}
