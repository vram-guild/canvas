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

package grondag.canvas.terrain;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

public class RenderRegionPruner {
	private boolean invalidateOccluder = false;
	private int occluderVersion = 0;
	private long cameraChunkPos;
	private int maxSquaredChunkDistance;
	public final CanvasFrustum frustum;
	public final TerrainOccluder occluder;

	// accessed from terrain iterator and render threads - holds regions to be closed on render thread
	private final SimpleUnorderedArrayList<BuiltRenderRegion> concurrentCloseList = new SimpleUnorderedArrayList<>();

	// holds close targets during close on iterator thread - avoid multiple lock attempts
	private final SimpleUnorderedArrayList<BuiltRenderRegion> prunerThreadCloseList = new SimpleUnorderedArrayList<>();

	// holds close targets during close on render thread - avoid multiple lock attempts
	private final SimpleUnorderedArrayList<BuiltRenderRegion> renderThreadCloseList = new SimpleUnorderedArrayList<>();

	public RenderRegionPruner(TerrainOccluder occluder) {
		this.occluder = occluder;
		frustum = occluder.frustum;
	}

	public void prepare(final long cameraChunkOrigin) {
		invalidateOccluder = false;
		cameraChunkPos = BlockPos.asLong(BlockPos.unpackLongX(cameraChunkOrigin) >> 4, BlockPos.unpackLongY(cameraChunkOrigin) >> 4, BlockPos.unpackLongZ(cameraChunkOrigin) >> 4);
		occluderVersion = occluder.version();
		maxSquaredChunkDistance = occluder.maxSquaredChunkDistance();
	}

	public void post() {
		if (!prunerThreadCloseList.isEmpty()) {
			synchronized (concurrentCloseList) {
				final int limit = prunerThreadCloseList.size();

				for (int i = 0; i < limit; ++i) {
					concurrentCloseList.add(prunerThreadCloseList.get(i));
				}
			}

			prunerThreadCloseList.clear();
		}
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	public long cameraChunkPos() {
		return cameraChunkPos;
	}

	public boolean didInvalidateOccluder() {
		return invalidateOccluder;
	}

	public void invalidateOccluder() {
		invalidateOccluder = true;
	}

	public void prune(BuiltRenderRegion r) {
		prunerThreadCloseList.add(r);
	}

	public int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}

	public void closeRegionsOnRenderThread() {
		if (!concurrentCloseList.isEmpty()) {
			synchronized (concurrentCloseList) {
				final int limit = concurrentCloseList.size();

				for (int i = 0; i < limit; ++i) {
					renderThreadCloseList.add(concurrentCloseList.get(i));
				}

				concurrentCloseList.clear();
			}
		}

		if (!renderThreadCloseList.isEmpty()) {
			final int limit = renderThreadCloseList.size();

			for (int i = 0; i < limit; ++i) {
				renderThreadCloseList.get(i).close();
			}

			renderThreadCloseList.clear();
		}
	}
}
