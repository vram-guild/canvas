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
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation;

public class VertexClusterRealm {
	public static final VertexClusterRealm SOLID = new VertexClusterRealm();
	public static final VertexClusterRealm TRANSLUCENT = new VertexClusterRealm();

	// WIP: allow for non-cubic clusters
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
		final int y = BlockPos.unpackLongY(packedOriginBlockPos);
		final int z = BlockPos.unpackLongZ(packedOriginBlockPos);
		return BlockPos.asLong(x >> BLOCKPOS_TO_CLUSTER_SHIFT, y >> BLOCKPOS_TO_CLUSTER_SHIFT, z >> BLOCKPOS_TO_CLUSTER_SHIFT);
	}

	private final Long2ObjectOpenHashMap<VertexCluster> clusters = new Long2ObjectOpenHashMap<>();
	private final ReferenceOpenHashSet<VertexCluster> clusterUpdates = new ReferenceOpenHashSet<>();

	private boolean isClosed = false;

	private VertexClusterRealm() { }

	public void clear() {
		assert RenderSystem.isOnRenderThread();

		for (VertexCluster clump : clusters.values()) {
			clump.close();
		}

		clusters.clear();
		clusterUpdates.clear();
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

		VertexCluster cluster = clusters.computeIfAbsent(storage.clusterPos, p -> new VertexCluster(VertexClusterRealm.this, p));
		clusterUpdates.add(cluster);
		return cluster.allocate(storage);
	}

	void notifyClosed(VertexCluster cluster) {
		assert RenderSystem.isOnRenderThread();
		VertexCluster deadCluster = clusters.remove(cluster.clusterPos);
		assert deadCluster != null : "Clump gone missing.";
	}

	public void update() {
		assert RenderSystem.isOnRenderThread();

		// WIP: regions now allocate immediate and compaction isn't every frame
		// Do we still need to have an update set and run this every frame?
		if (!clusterUpdates.isEmpty()) {
			for (VertexCluster clump : clusterUpdates) {
				clump.update();
			}

			clusterUpdates.clear();
		}

		// WIP: need a way to set the deadline appropriately based on steady frame rate and time already elapsed.
		// Method must ensure we don't have starvation - task queue can't grow indefinitely.
		ClusterTaskManager.run(System.nanoTime() + 2000000);
	}

	//private int lastFrame = 0;

	public String debugSummary() {
		if (clusters.isEmpty()) {
			return "Empty";
		}

		int total = 0;

		for (var cluster : clusters.values()) {
			total += cluster.activeBytes();
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

		return String.format("clusters:%d %dMb", clusters.size(), total / 0x100000);
	}
}
