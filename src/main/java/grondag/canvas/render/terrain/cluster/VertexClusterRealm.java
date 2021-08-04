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

package grondag.canvas.render.terrain.cluster;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation;
import grondag.canvas.render.terrain.drawlist.DrawListCullingHelper;

public class VertexClusterRealm {
	/**
	 * Number of bits we right-shift "chunk" coordinates to get cluster coordinates.
	 * Determines the size of clusters.
	 */
	private static final int CLUSTER_SHIFT = 2;

	/**
	 * Number of bits we right-shift block coordinates to get cluster coordinates.
	 */
	private static final int BLOCKPOS_TO_CLUSTER_SHIFT = 4 + CLUSTER_SHIFT;

	static long clusterPos(long packedOriginBlockPos) {
		final int x = BlockPos.unpackLongX(packedOriginBlockPos);
		final int z = BlockPos.unpackLongZ(packedOriginBlockPos);
		return BlockPos.asLong(x >> BLOCKPOS_TO_CLUSTER_SHIFT, 0, z >> BLOCKPOS_TO_CLUSTER_SHIFT);
	}

	private final Long2ObjectOpenHashMap<VertexCluster> clusters = new Long2ObjectOpenHashMap<>();

	private boolean isClosed = false;

	public final DrawListCullingHelper drawListCullingHelper;

	public final boolean isTranslucent;

	public VertexClusterRealm(DrawListCullingHelper drawListCullingHelper, boolean isTranslucent) {
		this.drawListCullingHelper = drawListCullingHelper;
		this.isTranslucent = isTranslucent;
	}

	public void clear() {
		assert RenderSystem.isOnRenderThread();

		for (VertexCluster cluster : clusters.values()) {
			cluster.close();
		}

		clusters.clear();
	}

	public void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;
			clear();
		}
	}

	RegionAllocation allocate(ClusteredDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();
		final VertexCluster cluster = clusters.computeIfAbsent(storage.clusterPos, p -> new VertexCluster(VertexClusterRealm.this, p));
		return cluster.allocate(storage);
	}

	void notifyClosed(VertexCluster cluster) {
		assert RenderSystem.isOnRenderThread();
		VertexCluster deadCluster = clusters.remove(cluster.clusterPos);
		assert deadCluster != null : "Clump gone missing.";
	}

	//private int lastFrame = 0;

	// WIP: make this faster
	public String debugSummary() {
		if (clusters.isEmpty()) {
			return "Empty";
		}

		long activeByes = 0;

		for (var cluster : clusters.values()) {
			activeByes += cluster.activeBytes();
		}

		//if (++lastFrame >= 200) {
		//	lastFrame = 0;
		//
		//	System.out.println();
		//	System.out.println("Allocation Report Follows");
		//
		//	for (var cluster : clusters.values()) {
		//		if (cluster.slabCount() == 1) {
		//			System.out.println("r:" + cluster.regionCount() + "  sl:" + cluster.slabCount() + "  occ:" + (cluster.activeBytes() * 100L) / (cluster.slabCount() * Slab.BYTES_PER_SLAB));
		//		}
		//	}
		//}

		return String.format("clusters: %d %dMb", clusters.size(), activeByes / 0x100000);
	}
}
