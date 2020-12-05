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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RenderRegionPruner implements Predicate<BuiltRenderRegion> {
	private boolean invalidateOccluder = false;
	private int occluderVersion = 0;
	private int cameraChunkX;
	private int cameraChunkY;
	private int cameraChunkZ;
	private int maxSquaredChunkDistance;
	public final CanvasFrustum frustum;
	public final TerrainOccluder occluder;

	// accessed from terrain iterator and render threads - holds regions to be closed on render thread
	private final ArrayBlockingQueue<BuiltRenderRegion> closeQueue = new ArrayBlockingQueue<>(4096);

	// holds close targets during close on render thread - avoid multiple lock attempts
	private final ObjectArrayList<BuiltRenderRegion> closeList = new ObjectArrayList<>();

	public RenderRegionPruner(TerrainOccluder occluder) {
		this.occluder = occluder;
		frustum = occluder.frustum;
	}

	public void prepare(final long cameraChunkOrigin) {
		invalidateOccluder = false;
		cameraChunkX = BlockPos.unpackLongX(cameraChunkOrigin) >> 4;
		cameraChunkY = BlockPos.unpackLongY(cameraChunkOrigin) >> 4;
		cameraChunkZ = BlockPos.unpackLongZ(cameraChunkOrigin) >> 4;
		occluderVersion = occluder.version();
		maxSquaredChunkDistance = occluder.maxSquaredChunkDistance();
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	public int cameraChunkX() {
		return cameraChunkX;
	}

	public int cameraChunkY() {
		return cameraChunkY;
	}

	public int cameraChunkZ() {
		return cameraChunkZ;
	}

	public boolean didInvalidateOccluder() {
		return invalidateOccluder;
	}

	public void invalidateOccluder() {
		invalidateOccluder = true;
	}

	@Override
	public boolean test(BuiltRenderRegion r) {
		if (!r.updateCameraDistanceAndVisibilityInfo(this)) {
			closeQueue.offer(r);
			return true;
		} else {
			return false;
		}
	}

	public int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}

	public void closeRegionsOnRenderThread() {
		if (closeQueue.isEmpty()) {
			return;
		}

		closeList.clear();
		closeQueue.drainTo(closeList);

		final int limit = closeList.size();

		for (int i = 0; i < limit; ++i) {
			closeList.get(i).close();
		}
	}
}
