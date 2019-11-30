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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;
import net.minecraft.client.render.chunk.ChunkRenderData;

import grondag.canvas.chunk.ChunkRebuildHelper;
import grondag.canvas.chunk.ChunkRenderDataExt;
import grondag.canvas.chunk.occlusion.ChunkOcclusionGraphExt;

@Mixin(ChunkRenderData.class)
public abstract class MixinChunkRenderData implements ChunkRenderDataExt {
	@Shadow
	private boolean[] nonEmpty; // has content
	@Shadow
	private boolean[] initialized;
	@Shadow
	private boolean empty;
	@Shadow
	private List<BlockEntity> blockEntities;
	@Shadow
	private ChunkOcclusionGraph occlusionGraph;

	@Shadow
	protected abstract void setNonEmpty(BlockRenderLayer blockRenderLayer);

	private int[][] collectorState;

	@Override
	public void canvas_setNonEmpty(BlockRenderLayer blockRenderLayer) {
		setNonEmpty(blockRenderLayer);
	}

	@Override
	public void canvas_clear() {
		empty = true;
		System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, nonEmpty, 0,
				ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
		System.arraycopy(ChunkRebuildHelper.EMPTY_RENDER_LAYER_FLAGS, 0, initialized, 0,
				ChunkRebuildHelper.BLOCK_RENDER_LAYER_COUNT);
		occlusionGraph.fill(false); // set all false
		((ChunkOcclusionGraphExt) occlusionGraph).canvas_visibilityData(null);
		collectorState = null;
		blockEntities.clear();
	}

	@Override
	public ChunkOcclusionGraphExt canvas_chunkVisibility() {
		return (ChunkOcclusionGraphExt) occlusionGraph;
	}

	@Override
	public int[][] canvas_collectorState() {
		return collectorState;
	}

	@Override
	public void canvas_collectorState(int[][] state) {
		collectorState = state;
	}
}
