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

import grondag.canvas.CanvasMod;
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

	private Slab[] slabs = new Slab[256]; // if we use more than this many we're already dead
	private int slabCount;
	/**
	 * Only one slab at a time is "hungry", meaning it can accept more vertex data.
	 * All other unclosed, non-null slabs (closing a slab will nullify it) are called
	 * "stuffed" slabs, but they may become "empty" over time as data become invalid.
	 * When a slab becomes more than half empty, we call it a "depleted" slab and it
	 * is eligible to be compacted to reduce draw calls.
	 */
	private @Nullable Slab hungrySlab = null;
	private int maxSlabCount = 0;
	private @Nullable Slab smallestDepletedSlab = null;

	private boolean isScheduled = false;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	private Slab getHungrySlabWithCapacity(int vertexCount) {
		if (hungrySlab == null) {
			claimNewNewHungrySlab();
		} else if (hungrySlab.availableVertexCount() < vertexCount) {
			updateSmallestDepletedSlab(hungrySlab);
			claimNewNewHungrySlab();
		}

		return hungrySlab;
	}

	private void claimNewNewHungrySlab() {
		hungrySlab = Slab.claim(claimOpenSlabIndex());
		slabs[hungrySlab.holdingClusterSlot()] = hungrySlab;
	}

	private int claimOpenSlabIndex() {
		if (slabCount++ < maxSlabCount) {
			for (int i = 0; i < maxSlabCount; ++i) {
				if (slabs[i] == null) {
					return i;
				}
			}

			throw new IllegalStateException("maxSlabCount is not accurate - no empty slab slots found");
		} else {
			return maxSlabCount++;
		}
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

			for (int i = 0; i < maxSlabCount; ++i) {
				if (slabs[i] != null) {
					slabs[i].release();
					slabs[i] = null;
				}
			}

			slabCount = 0;
			maxSlabCount = 0;
		}
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
			final int byteCount = noob.byteCount;

			//log.append("Added to allocated regions: ").append(noob.id).append("\n");
			Slab slab = getHungrySlabWithCapacity(noob.quadVertexCount);

			if (slab.allocateAndLoad(noob)) {
				allocatedRegions.add(noob);
				activeBytes += byteCount;
			} else {
				assert false : "Your code is bad and you should feel bad.";
				CanvasMod.LOG.error("Unable to allocate memory for render region. This is a bug.");
			}
		}

		//log.append("Noobs cleared after upload").append("\n");
		noobs.clear();
		newBytes = 0;

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

		if (allocatedRegions.remove(region)) {
			activeBytes -= region.byteCount;
		} else if (noobs.remove(region)) {
			newBytes -= region.byteCount;
			assert newBytes >= 0;
		} else {
			assert false : "Closure notification from region not in clump.";
		}

		final var slab = region.slab();
		assert slab != null;
		assert slabs[slab.holdingClusterSlot()] == slab;

		if (slab != hungrySlab) {
			if (slab.isEmpty()) {
				slabs[slab.holdingClusterSlot()] = null;
				slab.release();
				--slabCount;

				if (slab == smallestDepletedSlab) {
					smallestDepletedSlab = null;
					findSmallestDepletedSlab();
				}
			} else {
				// Save ourselves the trouble of searching for the smallest depleted slab during compact
				updateSmallestDepletedSlab(slab);
			}
		}

		if (allocatedRegions.isEmpty() && noobs.isEmpty()) {
			close();
			owner.notifyClosed(this);
		}
	}

	private void updateSmallestDepletedSlab(Slab slab) {
		assert slab != null;

		if (slab.isDepleted() && slab != smallestDepletedSlab) {
			if (smallestDepletedSlab == null || slab.usedVertexCount() < smallestDepletedSlab.usedVertexCount()) {
				smallestDepletedSlab = slab;
			}
		}
	}

	public int slabCount() {
		return slabCount;
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

	private boolean canCompact() {
		// We use a simple hueristic for compacting:
		// If the smallest depleted slab can fit into the
		// "next" hungry slab then compact.
		//
		// The "next" hungry slab is the current hungry slab if
		// it can accommodate the smallest depleted slab or
		// is assumed to be a new, empty slab if the current
		// hungry slab is stuffed.

		if (smallestDepletedSlab == null) {
			return false;
		}

		return hungrySlab == null
				|| hungrySlab.isStuffed()
				|| hungrySlab.availableVertexCount() >= smallestDepletedSlab.usedVertexCount();
	}

	/** Called after nullifying the smallest depleted slab to see if there is another. */
	private void findSmallestDepletedSlab() {
		assert smallestDepletedSlab == null;
		Slab result = null;

		if (slabCount > 1) {
			for (int i = 0; i < maxSlabCount; ++i) {
				Slab slab = slabs[i];

				if (slab != null && slab.isDepleted() && slab != hungrySlab) {
					if (result == null || slab.usedVertexCount() < result.usedVertexCount()) {
						result = slab;
					}
				}
			}
		}

		smallestDepletedSlab = result;
	}

	private void compact() {
		if (canCompact()) {
			retireSmallestDepletedSlab();

			// Schedule another pass if we can do it again
			if (canCompact()) {
				scheduleUpdate();
			}
		}
	}

	private void retireSmallestDepletedSlab() {
		assert smallestDepletedSlab != null;
		// WIP: remove
		//System.out.println("retiring depleted slab, new slab count will be " + (slabCount - 1));

		final Slab depleted = smallestDepletedSlab;
		final Slab hungry = getHungrySlabWithCapacity(depleted.usedVertexCount());

		for (ClusteredDrawableStorage region : depleted.regions()) {
			// Note we don't need to add to allocated or update byte total here
			// because would have been done when region was first allocated.
			assert allocatedRegions.contains(region);

			if (!hungry.transferFromSlab(region, depleted)) {
				assert false : "Your code is bad and you should feel bad.";
				CanvasMod.LOG.error("Unable to allocate memory for render region. This is a bug.");
			}
		}

		assert slabs[depleted.holdingClusterSlot()] == depleted;
		slabs[depleted.holdingClusterSlot()] = null;
		depleted.releaseTransferred();
		--slabCount;

		smallestDepletedSlab = null;
		findSmallestDepletedSlab();

		if (!holdingLists.isEmpty()) {
			for (var list : holdingLists) {
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
}
