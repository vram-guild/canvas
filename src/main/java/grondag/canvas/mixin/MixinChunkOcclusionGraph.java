/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.render.chunk.ChunkOcclusionData;

import grondag.canvas.chunk.occlusion.ChunkOcclusionGraphExt;
import grondag.canvas.chunk.occlusion.ChunkOcclusionMap;

@Mixin(ChunkOcclusionData.class)
public abstract class MixinChunkOcclusionGraph implements ChunkOcclusionGraphExt {
	private Object visibilityData = null;

	@Override
	public Object canvas_visibilityData() {
		return visibilityData;
	}

	@Override
	public void canvas_visibilityData(Object data) {
		canvas_releaseVisibilityData();
		visibilityData = data;
	}

	/** reuse arrays to prevent garbage build up */
	@Override
	public void canvas_releaseVisibilityData() {
		final Object prior = visibilityData;
		if (prior != null && prior instanceof ChunkOcclusionMap) {
			ChunkOcclusionMap.release((ChunkOcclusionMap) prior);
			visibilityData = null;
		}
	}

	@Override
	protected void finalize() {
		canvas_releaseVisibilityData();
	}
}
