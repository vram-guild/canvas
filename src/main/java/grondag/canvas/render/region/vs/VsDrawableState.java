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

package grondag.canvas.render.region.vs;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.region.base.AbstractDrawableState;
import grondag.canvas.varia.GFX;

public class VsDrawableState extends AbstractDrawableState<VsDrawableStorage> {
	private final int triVertexCount;

	public VsDrawableState(RenderState renderState, int quadVertexCount, VsDrawableStorage storage) {
		super(renderState, quadVertexCount, storage);
		triVertexCount = quadVertexCount() / 4 * 6;
	}

	public final int drawVertexCount() {
		return triVertexCount;
	}

	public void draw(int elementType, int indexBufferId) {
		assert !isClosed();
		GFX.drawElementsBaseVertex(DrawMode.QUADS.mode, triVertexCount, elementType, 0L, storage.baseVertex());
	}
}
