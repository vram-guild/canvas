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
	private int maxSlabCount = 0;
	private boolean isScheduled = false;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	private Slab getSlabWithCapacity(int vertexCount, Slab excluding) {
		if (slabCount > 0) {
			for (int i = 0; i < maxSlabCount; ++i) {
				Slab slab = slabs[i];

				if (slab != null && slab != excluding && slab.availableVertexCount() >= vertexCount) {
					return slab;
				}
			}
		}

		return claimNewNewEmptySlab();
	}

	private Slab claimNewNewEmptySlab() {
		Slab result = Slab.claim(claimOpenSlabIndex());
		slabs[result.holdingClusterSlot()] = result;
		return result;
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

			final int byteCount = noob.byteCount;

			//log.append("Added to allocated regions: ").append(noob.id).append("\n");
			Slab slab = getSlabWithCapacity(noob.quadVertexCount, null);

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

		if (slab.isEmpty()) {
			slabs[slab.holdingClusterSlot()] = null;
			slab.release();
			--slabCount;
		}

		if (allocatedRegions.isEmpty() && noobs.isEmpty()) {
			close();
			owner.notifyClosed(this);
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
		// Can we probably reduce the number of regions in use by at least one?

		if (slabCount < 2) {
			return false;
		}

		int depletedSlabVertexCount = 0;
		int capacitySlabVertexCount = 0;
		int removedSlabCount = 0;

		for (int i = 0; i < maxSlabCount; ++i) {
			final Slab slab = slabs[i];

			// first find slabs that are candidates - slabs with at least 1/4 slab waste
			if (slab == null) {
				continue;
			} else if (slab.vacatedVertexCount() > Slab.MAX_SLAB_QUAD_VERTEX_COUNT / 4) {
				depletedSlabVertexCount += slab.usedVertexCount();
				++removedSlabCount;
			} else {
				capacitySlabVertexCount += slab.availableVertexCount();
			}
		}

		if (removedSlabCount == 0) {
			return false;
		}

		int remainederVertexCount = depletedSlabVertexCount - capacitySlabVertexCount;

		if (remainederVertexCount > 0) {
			removedSlabCount -= (remainederVertexCount + Slab.BYTES_PER_SLAB - 1) / Slab.BYTES_PER_SLAB;
		}

		assert removedSlabCount >= 0;
		return removedSlabCount > 0;
	}

	private void compact() {
		if (canCompact()) {
			tryCompact(false);
		}
	}

	private void tryCompact(boolean simulate) {
		final int startingCount = slabCount;

		for (int i = 0; i < maxSlabCount; ++i) {
			final Slab source = slabs[i];

			// reallocate slabs with at least 1/4 slab waste
			if (source != null && source.vacatedVertexCount() > Slab.MAX_SLAB_QUAD_VERTEX_COUNT / 4) {
				for (ClusteredDrawableStorage region : source.regions()) {
					// Note we don't need to add to allocated or update byte total here
					// because would have been done when region was first allocated.
					assert allocatedRegions.contains(region);

					if (!getSlabWithCapacity(region.quadVertexCount, source).transferFromSlab(region, source)) {
						assert false : "Your code is bad and you should feel bad.";
						CanvasMod.LOG.error("Unable to allocate memory for render region. This is a bug.");
					}
				}

				assert slabs[source.holdingClusterSlot()] == source;
				slabs[source.holdingClusterSlot()] = null;
				source.releaseTransferred();
				--slabCount;
			}
		}

		if (startingCount - slabCount > 0 && !holdingLists.isEmpty()) {
			for (var list : holdingLists) {
				list.invalidate();
			}
		}

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
