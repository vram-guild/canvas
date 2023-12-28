/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.render.terrain.cluster;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.core.BlockPos;

import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation;

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
		final int x = BlockPos.getX(packedOriginBlockPos);
		final int z = BlockPos.getZ(packedOriginBlockPos);
		return BlockPos.asLong(x >> BLOCKPOS_TO_CLUSTER_SHIFT, 0, z >> BLOCKPOS_TO_CLUSTER_SHIFT);
	}

	private final Long2ObjectOpenHashMap<VertexCluster> clusters = new Long2ObjectOpenHashMap<>();

	private boolean isClosed = false;

	public final boolean isTranslucent;

	public VertexClusterRealm(boolean isTranslucent) {
		this.isTranslucent = isTranslucent;
	}

	public void clear() {
		assert RenderSystem.isOnRenderThread();

		// loop through static array to prevent concurrent removal issue
		final VertexCluster[] clusterArray = clusters.values().toArray(new VertexCluster[0]);

		for (final VertexCluster cluster : clusterArray) {
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
		final VertexCluster deadCluster = clusters.remove(cluster.clusterPos);
		assert deadCluster != null : "Clump gone missing.";
	}

	//private int lastFrame = 0;

	// WIP: make this faster
	public String debugSummary() {
		if (clusters.isEmpty()) {
			return "Empty";
		}

		long activeByes = 0;

		for (final var cluster : clusters.values()) {
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
