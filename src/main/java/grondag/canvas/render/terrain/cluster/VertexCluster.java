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
import grondag.canvas.render.terrain.drawlist.ClusterDrawList;
import grondag.canvas.render.terrain.drawlist.SlabIndex;
import grondag.canvas.render.terrain.drawlist.TerrainVAO;

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
	private boolean itMe = false;

	public VertexCluster(VertexClusterRealm owner, long clusterPos) {
		realm = owner;
		this.clusterPos = clusterPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	private Slab getHungrySlab(int slabBytes) {
		if (hungrySlab == null || hungrySlab.availableBytes() < slabBytes) {
			// We want to use the new slab for compaction so request one big enough to hold everything we have
			hungrySlab = SlabAllocator.claim(activeBytes + slabBytes);
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
				itMe = true;

				for (final RegionAllocation alloc : allocatedRegions.values()) {
					alloc.closeRegion();
				}

				allocatedRegions.clear();
				itMe = false;
			}

			for (final var slab : slabs) {
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

		final var result = new RegionAllocation(region);
		scheduleIfNeeded();
		return result;
	}

	/** For assertion checks only. */
	boolean isPresent(ClusteredDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();
		return allocatedRegions.containsKey(storage);
	}

	private void scheduleIfNeeded() {
		if (!isClosed && slabs.size() > 1 && !isScheduled) {
			isScheduled = true;
			ClusterTaskManager.schedule(this);
		}
	}

	public int slabCount() {
		return slabs.size();
	}

	@Override
	public boolean run(long deadlineNanos) {
		if (isScheduled) {
			isScheduled = false;
			compact();
		}

		return true;
	}

	private void compact() {
		if (slabs.size() < 2) {
			// nothing to do
			return;
		}

		// NB: hungry slab can't be null here because we have at least two slabs. But
		// it may not be big enough. Ensure hungry slab can hold everything, including own contents
		assert hungrySlab.usedBytes() >= 0;
		assert hungrySlab.usedBytes() <= hungrySlab.capacityBytes();

		if (hungrySlab.availableBytes() < activeBytes - hungrySlab.usedBytes()) {
			hungrySlab = SlabAllocator.claim(activeBytes);
			slabs.add(hungrySlab);
		}

		final Slab hungrySlab = this.hungrySlab;

		for (final var region : allocatedRegions.values()) {
			final var oldAllocation = region.getAllocation();

			if (oldAllocation.slab != hungrySlab) {
				final var newAllocation = hungrySlab.transferFromSlabAllocation(region.factory, oldAllocation);
				region.setAllocation(newAllocation);
				oldAllocation.release();
			}
		}

		assert slabs.size() == 1;
		assert slabs.get(0) == hungrySlab;

		if (!holdingLists.isEmpty()) {
			for (final var list : holdingLists) {
				list.invalidate();
			}
		}
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
		private SlabAllocation slabAllocation;

		private final SlabAllocationFactory factory = (s, b, q) -> {
			return new SlabAllocation(s, b, q);
		};

		private RegionAllocation(ClusteredDrawableStorage region) {
			this.region = region;
			final var transferBuffer = region.getAndClearTransferBuffer();
			slabAllocation = getHungrySlab(region.byteCount).allocateAndLoad(factory, transferBuffer);
			assert slabAllocation.quadVertexCount == region.quadVertexCount;
			transferBuffer.release();
			allocatedRegions.put(region, this);
			activeBytes += region.byteCount;
		}

		private void closeRegion() {
			region.close();
			assert slabAllocation == null : "Region close did not release slab allocations";
		}

		public void onRegionClosed() {
			if (slabAllocation != null) {
				slabAllocation.release();
				slabAllocation = null;
			}

			// Don't remove from map when closing - avoids CME
			if (!itMe && allocatedRegions.remove(region) == null) {
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

		public SlabAllocation getAllocation() {
			return slabAllocation;
		}

		public void setAllocation(SlabAllocation allocation) {
			slabAllocation = allocation;
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
			private boolean isSlabAllocationClosed = false;

			private SlabAllocation(Slab slab, int baseQuadVertexIndex, int quadVertexCount) {
				triVertexCount = quadVertexCount * 6 / 4;
				this.slab = slab;
				this.baseQuadVertexIndex = baseQuadVertexIndex;
				this.quadVertexCount = quadVertexCount;

				vao = new TerrainVAO(() -> slab.glBufferId(), () -> SlabIndex.get().glBufferId(), baseQuadVertexIndex);
			}

			public ClusteredDrawableStorage region() {
				return region;
			}

			public void bind() {
				vao.bind();
			}

			public void release() {
				assert !isSlabAllocationClosed;

				if (!isSlabAllocationClosed) {
					isSlabAllocationClosed = true;
					vao.shutdown();
					slab.removeAllocation(this);
					scheduleIfNeeded();

					if (slab.isEmpty() && slab != hungrySlab) {
						if (slabs.remove(slab)) {
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
