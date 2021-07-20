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

public class VertexCluster implements ClusterTask {
	//final StringBuilder log = new StringBuilder();

	public final VertexClusterRealm owner;
	private final ObjectArrayList<ClusteredDrawableStorage> noobs = new ObjectArrayList<>();
	private final ReferenceOpenHashSet<ClusteredDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();

	private boolean isClosed = false;

	private int activeBytes = 0;
	private int newBytes = 0;

	final long clumpPos;

	private Slab[] slabs = new Slab[256]; // if we use more than this many we're already dead
	private int slabCount;
	private int openSlabIndex = -1;
	private int maxSlabCount = 0;

	private boolean isScheduled = false;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	int activeBytes() {
		return activeBytes;
	}

	private Slab findSlabWithCapacity(int vertexCount) {
		int result = openSlabIndex;

		if (result == -1 || slabs[result].availableVertexCount() < vertexCount) {
			result = claimOpenSlabIndex();
			openSlabIndex = result;
			slabs[result] = Slab.claim();
		}

		return slabs[result];
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
		close(true);
	}

	void close(boolean notify) {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			activeBytes = 0;
			newBytes = 0;

			if (!allocatedRegions.isEmpty()) {
				for (ClusteredDrawableStorage region : allocatedRegions) {
					region.close(notify);
				}

				allocatedRegions.clear();
			}

			if (!noobs.isEmpty()) {
				for (ClusteredDrawableStorage region : noobs) {
					region.close(notify);
				}

				noobs.clear();
			}

			for (int i = 0; i < maxSlabCount; ++i) {
				if (slabs[i] != null) {
					slabs[i].release();
					slabs[i] = null;
				}
			}
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
			Slab slab = findSlabWithCapacity(noob.quadVertexCount);

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
		return activeBytes < (slabCount - 1) * Slab.BYTES_PER_SLAB;
	}

	private void compact() {
		// WIP Do better stuff
		for (int i = 0; i < maxSlabCount; ++i) {
			Slab slab = slabs[i];

			if (slab != null && slab.isEmpty()) {
				slab.release();
				slabs[i] = null;
			}
		}
	}
}
