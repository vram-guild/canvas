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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.render.terrain.cluster.ClusterTaskManager.ClusterTask;
import grondag.canvas.render.terrain.cluster.drawlist.ClusterDrawList;

public class VertexCluster implements ClusterTask {
	//final StringBuilder log = new StringBuilder();
	private final ReferenceOpenHashSet<ClusterDrawList> holdingLists = new ReferenceOpenHashSet<>();
	public final VertexClusterRealm owner;
	private final ObjectArrayList<ClusteredDrawableStorage> noobs = new ObjectArrayList<>();
	private final ReferenceOpenHashSet<ClusteredDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();

	private boolean isClosed = false;

	private int activeBytes = 0;
	private int newBytes = 0;

	final long clumpPos;

	private ObjectArrayList<Slab> slabs = new ObjectArrayList<>();
	private @Nullable Slab hungrySlab = null;
	private boolean isScheduled = false;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	private boolean validateAllocation() {
		int regionTotalBytes = 0, slabActiveBytes = 0, slabAllocationBytes = 0;

		for (var region : allocatedRegions) {
			int regionBytes = 0;
			regionTotalBytes += region.byteCount;

			for (var slabAllocation : region.allocations()) {
				final int bytes = slabAllocation.quadVertexCount() * SlabAllocator.BYTES_PER_SLAB_VERTEX;
				regionBytes += bytes;
				slabAllocationBytes += bytes;
			}

			assert regionBytes == region.byteCount;
		}

		for (var slab : slabs) {
			slabActiveBytes += slab.usedBytes();
		}

		final boolean result = activeBytes == slabActiveBytes
				&& regionTotalBytes == activeBytes
				&& slabAllocationBytes == activeBytes;
		assert result;
		return result;
	}

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
			newBytes = 0;

			if (!allocatedRegions.isEmpty()) {
				for (ClusteredDrawableStorage region : allocatedRegions) {
					region.close(false);
				}

				allocatedRegions.clear();
			}

			if (!noobs.isEmpty()) {
				for (ClusteredDrawableStorage region : noobs) {
					region.close(false);
				}

				noobs.clear();
			}

			for (var slab : slabs) {
				slab.release();
			}

			slabs.clear();
		}
	}

	int regionCount() {
		return noobs.size() + allocatedRegions.size();
	}

	void allocate(ClusteredDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();

		//log.append("Added to noobs: ").append(storage.id).append("\n");
		noobs.add(storage);
		storage.setCluster(this);
		newBytes += storage.byteCount;

		//assert isPresent(storage);
	}

	/** For assertion checks only. */
	boolean isPresent(ClusteredDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();
		return allocatedRegions.contains(storage) || noobs.contains(storage);
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		if (newBytes == 0) return;

		assert !noobs.isEmpty();

		for (ClusteredDrawableStorage noob : noobs) {
			if (noob.isClosed()) {
				continue;
			}

			int remainingVertexCount = noob.quadVertexCount;
			final var transferBuffer = noob.getAndClearTransferBuffer();

			while (remainingVertexCount > 0) {
				remainingVertexCount -= getHungrySlab(remainingVertexCount).allocateAndLoad(noob, transferBuffer, noob.quadVertexCount - remainingVertexCount);
			}

			assert remainingVertexCount == 0;
			transferBuffer.release();
			allocatedRegions.add(noob);
			activeBytes += noob.byteCount;
		}

		//log.append("Noobs cleared after upload").append("\n");
		noobs.clear();
		newBytes = 0;

		// WIP: remove - expensive even for dev
		assert validateAllocation();

		if (canCompact()) {
			scheduleUpdate();
		}
	}

	/**
	 * Region will call after it releases its allocation from
	 * slab but before it nullifies the slab reference.
	 */
	void notifyClosed(ClusteredDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		//log.append("Notify closed: ").append(region.id).append("\n");

		// NB: Remove allocations before regions so the assertion the region is present doesn't fail.
		for (var slabAllocation : region.allocations()) {
			var slab = slabAllocation.slab();
			assert slab != null;

			if (slab.isEmpty()) {
				if (slabs.remove(slab)) {
					slab.release();
				} else {
					assert false : "Region slab not found on region close";
				}
			}
		}

		if (allocatedRegions.remove(region)) {
			activeBytes -= region.byteCount;
		} else if (noobs.remove(region)) {
			newBytes -= region.byteCount;
			assert newBytes >= 0;
		} else {
			assert false : "Closure notification from region not in clump.";
		}

		if (allocatedRegions.isEmpty() && noobs.isEmpty()) {
			close();
			owner.notifyClosed(this);
		}

		// WIP: remove - expensive even for dev
		assert validateAllocation();
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

		for (var region : allocatedRegions) {
			for (var allocation : region.prepareForReallocation()) {
				int transferStart = 0;

				while (transferStart < allocation.quadVertexCount()) {
					if (targetSlab.isFull()) {
						targetSlab = SlabAllocator.claim(remainingBytes);
						newSlabs.add(targetSlab);
					}

					final int transferredVertexCount = targetSlab.transferFromSlabAllocation(region, allocation, transferStart);
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

		// WIP: remove
		//System.out.println("Compact, slabs retired: " + (startingCount - slabCount));
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
}
