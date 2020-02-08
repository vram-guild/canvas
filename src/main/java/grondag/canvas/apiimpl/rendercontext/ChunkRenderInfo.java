/*******************************************************************************
 * Copyright 2019, 2020 grondag
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


package grondag.canvas.apiimpl.rendercontext;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.util.math.BlockPos;

import grondag.canvas.chunk.RegionData;

/**
 * Holds, manages and provides access to the chunk-related state
 * needed by fallback and mesh consumers during terrain rendering.
 *
 * <p>Exception: per-block position offsets are tracked here so they can
 * be applied together with chunk offsets.
 *
 * TODO: remove or slim down/focus
 */
public class ChunkRenderInfo {

	private final BlockPos.Mutable chunkOrigin = new BlockPos.Mutable();
	RegionData chunkData;
	BlockBufferBuilderStorage builders;

	private final Object2ObjectOpenHashMap<RenderLayer, BufferBuilder> buffers = new Object2ObjectOpenHashMap<>();

	void prepare(RegionData chunkData, BlockBufferBuilderStorage builders, BlockPos origin) {
		chunkOrigin.set(origin);
		this.chunkData = chunkData;
		this.builders = builders;
		buffers.clear();
	}

	void release() {
		chunkData = null;
		buffers.clear();
	}

	/** Lazily retrieves output buffer for given layer, initializing as needed. */
	public BufferBuilder getInitializedBuffer(RenderLayer renderLayer) {
		BufferBuilder result = buffers.get(renderLayer);

		if (result == null) {
			result = builders.get(renderLayer);
			chunkData.markPopulated(renderLayer);
			buffers.put(renderLayer, result);

			if (chunkData.markInitialized(renderLayer)) {
				result.begin(7, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
			}
		}

		return result;
	}
}
