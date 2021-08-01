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

import java.util.IdentityHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.render.terrain.cluster.ClusterTaskManager.ClusterTask;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;
import grondag.canvas.render.terrain.cluster.drawlist.ClusterDrawList;
import grondag.canvas.render.terrain.cluster.drawlist.SlabIndex;
import grondag.canvas.render.terrain.cluster.drawlist.TerrainVAO;

public class VertexCluster implements ClusterTask {
	private final ReferenceOpenHashSet<ClusterDrawList> holdingLists = new ReferenceOpenHashSet<>();
	public final VertexClusterRealm realm;
	private final IdentityHashMap<ClusteredDrawableStorage, RegionAllocation> allocatedRegions = new IdentityHashMap<>();

	private boolean isClosed = false;
	private int activeBytes = 0;
	final long clusterPos;

	private ObjectArrayList<Slab> slabs = new ObjectArrayList<>();
	private @Nullable Slab hungrySlab = null;
	private boolean isScheduled = false;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		realm = owner;
		clusterPos = clumpPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	// Kept for testing, expensive even for dev...
	//private boolean validateAllocation() {
	//	final var slabMap = new Object2IntOpenHashMap<Slab>();
	//	int regionTotalBytes = 0, slabAllocationBytes = 0;
	//
	//	for (var allocRegion : allocatedRegions.values()) {
	//		int regionBytes = 0;
	//		regionTotalBytes += allocRegion.region.byteCount;
	//		assert !allocRegion.region.isClosed();
	//
	//		for (var slabAllocation : allocRegion.allocations()) {
	//			final int bytes = slabAllocation.quadVertexCount * SlabAllocator.BYTES_PER_SLAB_VERTEX;
	//			regionBytes += bytes;
	//			slabAllocationBytes += bytes;
	//
	//			if (!slabMap.containsKey(slabAllocation.slab)) {
	//				slabMap.put(slabAllocation.slab, 0);
	//			}
	//
	//			slabMap.addTo(slabAllocation.slab, bytes);
	//		}
	//
	//		assert regionBytes == allocRegion.region.byteCount;
	//	}
	//
	//	assert regionTotalBytes == activeBytes;
	//	assert slabAllocationBytes == activeBytes;
	//	assert slabMap.size() == slabs.size();
	//	int slabActiveBytes = 0;
	//
	//	for (var slab : slabs) {
	//		slabActiveBytes += slab.usedBytes();
	//		assert slab.usedBytes() == slabMap.getInt(slab);
	//	}
	//
	//	assert slabActiveBytes == activeBytes;
	//	return true;
	//}

	private Slab getHungrySlab(int vertexCount) {
		if (hungrySlab == null || hungrySlab.isFull()) {
			hungrySlab = SlabAllocator.claim(vertexCount * SlabAllocator.BYTES_PER_SLAB_VERTEX);
			slabs.add(hungrySlab);
		}

		return hungrySlab;
	}

	void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			activeBytes = 0;

			if (!allocatedRegions.isEmpty()) {
				for (RegionAllocation alloc : allocatedRegions.values()) {
					alloc.closeRegion();
				}

				allocatedRegions.clear();
			}

			for (var slab : slabs) {
				slab.release();
			}

			slabs.clear();

			realm.notifyClosed(this);
		}
	}

	int regionCount() {
		return allocatedRegions.size();
	}

	RegionAllocation allocate(ClusteredDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		if (region.isClosed()) {
			return null;
		}

		return new RegionAllocation(region);
	}

	/** For assertion checks only. */
	boolean isPresent(ClusteredDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();
		return allocatedRegions.containsKey(storage);
	}

	public void update() {
		//assert validateAllocation();

		if (canCompact()) {
			scheduleUpdate();
		}
	}

	public int slabCount() {
		return slabs.size();
	}

	private void scheduleUpdate() {
		if (!isScheduled) {
			isScheduled = true;
			ClusterTaskManager.schedule(this);
		}
	}

	@Override
	public boolean run(long deadlineNanos) {
		if (isScheduled) {
			isScheduled = false;
			compact();
		}

		return true;
	}

	/**
	 * True if we can reduce the number of small slabs
	 * or remove at least one small slab of vacated capacity.
	 */
	private boolean canCompact() {
		if (isClosed || slabs.size() < 2) {
			return false;
		}

		int slabCapacity = 0, slabActiveBytes = 0, smallCount = 0;

		for (var slab : slabs) {
			slabCapacity += slab.capacityBytes();
			slabActiveBytes += slab.usedBytes();

			if (slab.allocator.isSmall) {
				++smallCount;
			}
		}

		assert activeBytes == slabActiveBytes : "Cluster and slabs do not agree on allocated capacity";

		return (slabCapacity - activeBytes) >= SlabAllocator.SMALL_SLAB_BYTES
				|| smallCount >= 3 && activeBytes >= SlabAllocator.LARGE_SLAB_BYTE_THRESHOLD;
	}

	private void compact() {
		if (canCompact()) {
			tryCompact();
		}
	}

	private void tryCompact() {
		final ObjectArrayList<Slab> newSlabs = new ObjectArrayList<>();
		int remainingBytes = activeBytes;
		Slab targetSlab = SlabAllocator.claim(remainingBytes);
		newSlabs.add(targetSlab);

		for (var region : allocatedRegions.values()) {
			for (var allocation : region.prepareForReallocation()) {
				int transferStart = 0;

				while (transferStart < allocation.quadVertexCount) {
					if (targetSlab.isFull()) {
						targetSlab = SlabAllocator.claim(remainingBytes);
						newSlabs.add(targetSlab);
					}

					final var alloc = targetSlab.transferFromSlabAllocation(region.factory, allocation, transferStart);
					final int transferredVertexCount = alloc.quadVertexCount;
					transferStart += transferredVertexCount;
					remainingBytes -= transferredVertexCount * SlabAllocator.BYTES_PER_SLAB_VERTEX;
				}
			}
		}

		assert remainingBytes == 0;

		for (var oldSlab : slabs) {
			oldSlab.releaseTransferred();
		}

		if (!holdingLists.isEmpty()) {
			for (var list : holdingLists) {
				list.invalidate();
			}
		}

		hungrySlab = targetSlab;
		slabs.clear();
		slabs = newSlabs;
	}

	void addListListener(ClusterDrawList listener) {
		assert !holdingLists.contains(listener);
		assert !isClosed;
		holdingLists.add(listener);
	}

	void removeListListener(ClusterDrawList listener) {
		assert holdingLists.contains(listener);
		assert !isClosed;
		holdingLists.remove(listener);
	}

	public class RegionAllocation {
		public final ClusteredDrawableStorage region;
		private ObjectArrayList<SlabAllocation> slabAllocations = new ObjectArrayList<>();

		private final SlabAllocationFactory factory = (s, b, q) -> {
			final var result = new SlabAllocation(s, b, q);
			slabAllocations.add(result);
			return result;
		};

		private RegionAllocation(ClusteredDrawableStorage region) {
			this.region = region;

			int remainingVertexCount = region.quadVertexCount;
			final var transferBuffer = region.getAndClearTransferBuffer();

			while (remainingVertexCount > 0) {
				SlabAllocation alloc = getHungrySlab(remainingVertexCount).allocateAndLoad(factory, transferBuffer, region.quadVertexCount - remainingVertexCount);
				remainingVertexCount -= alloc.quadVertexCount;
			}

			assert remainingVertexCount == 0;
			transferBuffer.release();
			allocatedRegions.put(region, this);
			activeBytes += region.byteCount;
		}

		private void closeRegion() {
			region.close();
			assert slabAllocations.isEmpty() : "Region close did not release slab allocations";
		}

		public void onRegionClosed() {
			for (var alloc : slabAllocations) {
				alloc.release();
			}

			slabAllocations.clear();

			if (allocatedRegions.remove(region) == null) {
				assert false : "Closure notification from region not in cluster.";
			} else {
				activeBytes -= region.byteCount;
			}

			if (allocatedRegions.isEmpty()) {
				close();
			} else {
				//assert validateAllocation();
			}
		}

		/**
		 * Returns prior allocations and clears current.
		 */
		private ObjectArrayList<SlabAllocation> prepareForReallocation() {
			assert !slabAllocations.isEmpty();
			final var result = slabAllocations;
			slabAllocations = new ObjectArrayList<>();
			return result;
		}

		public ObjectArrayList<SlabAllocation> allocations() {
			return slabAllocations;
		}

		public VertexCluster cluster() {
			return VertexCluster.this;
		}

		public class SlabAllocation {
			public final Slab slab;
			public final int baseQuadVertexIndex;
			public final int quadVertexCount;
			public final int triVertexCount;
			private final TerrainVAO vao;
			private boolean isClosed = false;

			private SlabAllocation(Slab slab, int baseQuadVertexIndex, int quadVertexCount) {
				triVertexCount = quadVertexCount * 6 / 4;
				this.slab = slab;
				this.baseQuadVertexIndex = baseQuadVertexIndex;
				this.quadVertexCount = quadVertexCount;

				vao = new TerrainVAO(() -> slab.glBufferId(), () -> SlabIndex.get().glBufferId(), baseQuadVertexIndex);
			}

			public void bind() {
				vao.bind();
			}

			public void release() {
				assert !isClosed;

				if (!isClosed) {
					isClosed = true;

					vao.shutdown();

					slab.removeAllocation(this);

					if (slab.isEmpty()) {
						if (slabs.remove(slab)) {
							// If the slab is the hungry slab we need to clear it
							// so that we don't try to add to after release.
							if (slab == hungrySlab) {
								hungrySlab = null;
							}

							slab.release();
						} else {
							assert false : "Slab not found on empty";
						}
					}
				}
			}
		}
	}

	@FunctionalInterface
	public interface SlabAllocationFactory {
		SlabAllocation create(Slab slab, int baseQuadVertexIndex, int quadVertexCount);
	}
}
