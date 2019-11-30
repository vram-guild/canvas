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

/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.chunk;

import net.minecraft.block.Block.OffsetType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderTask;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderInfo;
import grondag.canvas.light.LightSmoother;

//PERF: many opportunities here

/**
 * Holds, manages and provides access to the chunk-related state needed by
 * fallback and mesh consumers during terrain rendering.
 * <p>
 *
 * Exception: per-block position offsets are tracked here so they can be applied
 * together with chunk offsets.
 */
public class ChunkRenderInfo {
	private final BlockRenderInfo blockInfo;
	ChunkRenderTask chunkTask;
	ChunkRenderData chunkData;
	ChunkRenderer chunkRenderer;
	FastRenderRegion blockView;

	// model offsets for plants, etc.
	private boolean hasOffsets = false;
	private float offsetX = 0;
	private float offsetY = 0;
	private float offsetZ = 0;

	public ChunkRenderInfo(BlockRenderInfo blockInfo) {
		this.blockInfo = blockInfo;
	}

	public void setBlockView(FastRenderRegion blockView) {
		this.blockView = blockView;
	}

	public void setChunkTask(ChunkRenderTask chunkTask) {
		this.chunkTask = chunkTask;
	}

	//    private static final ConcurrentPerformanceCounter counter = new ConcurrentPerformanceCounter();

	// stable samples - original version
	//    [05:49:13] [Chunk Batcher 9/INFO]: time this sample = 9.480s for 2,000 items @ 4740143ns each.
	//    [05:49:25] [Chunk Batcher 9/INFO]: time this sample = 9.260s for 2,000 items @ 4630223ns each.
	//    [05:49:25] [Chunk Batcher 10/INFO]: time this sample = 9.265s for 2,001 items @ 4630319ns each.
	//    [05:49:35] [Chunk Batcher 11/INFO]: time this sample = 10.096s for 2,000 items @ 5047808ns each.
	//    [05:49:43] [Chunk Batcher 12/INFO]: time this sample = 10.832s for 2,000 items @ 5416180ns each.
	//    [05:49:52] [Chunk Batcher 14/INFO]: time this sample = 9.635s for 2,000 items @ 4817543ns each.
	//    [05:49:52] [Chunk Batcher 11/INFO]: time this sample = 9.641s for 2,001 items @ 4818034ns each.
	//    [05:49:52] [Chunk Batcher 12/INFO]: time this sample = 9.648s for 2,002 items @ 4819068ns each.

	// after integer math + simpler indexing
	//    [07:40:51] [Chunk Batcher 6/INFO]: time this sample = 8.103s for 2,000 items @ 4051533ns each.
	//    [07:40:51] [Chunk Batcher 1/INFO]: time this sample = 8.106s for 2,001 items @ 4051069ns each.
	//    [07:40:58] [Chunk Batcher 0/INFO]: time this sample = 7.770s for 2,000 items @ 3884816ns each.
	//    [07:41:06] [Chunk Batcher 6/INFO]: time this sample = 5.997s for 2,000 items @ 2998444ns each.
	//    [07:41:39] [Chunk Batcher 1/INFO]: time this sample = 6.762s for 2,000 items @ 3380933ns each.
	//    [07:42:01] [Chunk Batcher 7/INFO]: time this sample = 6.655s for 2,000 items @ 3327388ns each.

	public void prepare(ChunkRenderer chunkRenderer, BlockPos.Mutable chunkOrigin) {
		chunkData = chunkTask.getRenderData();
		this.chunkRenderer = chunkRenderer;
		if(Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			LightSmoother.computeSmoothedBrightness(chunkOrigin, blockView, blockView.brightnessCache);

			//            counter.endRun(start);
			//            counter.addCount(1);
			//            if(counter.runCount() >= 2000) {
			//                CanvasMod.LOG.info(counter.stats());
			//                counter.clearStats();
			//            }
		}
	}

	public void release() {
		blockView.release();
		blockView = null;
		chunkData = null;
		chunkTask = null;
		chunkRenderer = null;
	}

	public void beginBlock() {
		final BlockState blockState = blockInfo.blockState;
		final BlockPos blockPos = blockInfo.blockPos;

		if (blockState.getBlock().getOffsetType() == OffsetType.NONE) {
			hasOffsets = false;
		} else {
			hasOffsets = true;
			final Vec3d offset = blockState.getOffsetPos(blockInfo.blockView, blockPos);
			offsetX = (float) offset.x;
			offsetY = (float) offset.y;
			offsetZ = (float) offset.z;
		}
	}

	/**
	 * Applies position offset for chunk and, if present, block random offset.
	 */
	public void applyOffsets(MutableQuadViewImpl q) {
		if(hasOffsets) {
			for (int i = 0; i < 4; i++) {
				q.pos(i, q.x(i) + offsetX, q.y(i) + offsetY, q.z(i) + offsetZ);
			}
		}
	}

	public int cachedBrightness(BlockPos pos) {
		return blockView.cachedBrightness(pos);
	}

	public float cachedAoLevel(BlockPos pos) {
		return blockView.cachedAoLevel(pos);
	}
}
