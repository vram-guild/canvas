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

import static grondag.canvas.render.terrain.cluster.SlabAllocator.BYTES_PER_SLAB_VERTEX;

import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.varia.GFX;

public class Slab extends AbstractGlBuffer {
	final SlabAllocator allocator;
	private int headVertexIndex = 0;
	private boolean isClaimed;
	private int vaoBufferId = 0;
	private final ReferenceOpenHashSet<ClusteredDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();
	private int usedVertexCount;
	/** Used by vertex cluster to efficiently locate slab when there are notifications from regions. */
	private int holdingClusterSlot = -1;

	Slab(SlabAllocator allocator) {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(allocator.bytesPerSlab, GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		this.allocator = allocator;
		assert RenderSystem.isOnRenderThread();
	}

	/** How much vertex capacity is remaining. */
	int availableVertexCount() {
		assert RenderSystem.isOnRenderThread();
		return allocator.maxSlabQuadVertexCount - headVertexIndex;
	}

	int usedVertexCount() {
		return usedVertexCount;
	}

	public int vacatedVertexCount() {
		return headVertexIndex - usedVertexCount;
	}

	int holdingClusterSlot() {
		return holdingClusterSlot;
	}

	boolean isEmpty() {
		assert RenderSystem.isOnRenderThread();
		return usedVertexCount == 0;
	}

	boolean isVacated() {
		return vacatedVertexCount() > allocator.vacatedQuadVertexThreshold;
	}

	Set<ClusteredDrawableStorage> regions() {
		assert RenderSystem.isOnRenderThread();
		return allocatedRegions;
	}

	void prepareForClaim(int slot) {
		assert !isClaimed;
		assert holdingClusterSlot == -1;
		assert usedVertexCount == 0;
		holdingClusterSlot = slot;
		isClaimed = true;
	}

	void release() {
		assert allocatedRegions.isEmpty();
		assert usedVertexCount == 0;
		releaseInner();
	}

	private void addToVertexCounts(int vertexCount) {
		usedVertexCount += vertexCount;
		allocator.addToVertexCount(vertexCount);
	}

	/** Doesn't assume or require all regions to have closed. */
	void releaseTransferred() {
		allocatedRegions.clear();
		releaseInner();
	}

	private void releaseInner() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		assert holdingClusterSlot >= 0;
		headVertexIndex = 0;
		isClaimed = false;
		holdingClusterSlot = -1;
		addToVertexCounts(-usedVertexCount);
		allocator.release(this);
	}

	/** Returns true if successful, or false if capacity would be exceeded. */
	boolean allocateAndLoad(ClusteredDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		final int newHeadVertexIndex = headVertexIndex + region.quadVertexCount;

		if (newHeadVertexIndex > allocator.maxSlabQuadVertexCount) {
			return false;
		}

		assert !allocatedRegions.contains(region);
		allocatedRegions.add(region);
		region.setLocation(this, headVertexIndex);
		addToVertexCounts(region.quadVertexCount);

		GFX.bindBuffer(bindTarget, glBufferId());
		region.getAndClearTransferBuffer().releaseToBoundBuffer(GFX.GL_ARRAY_BUFFER, headVertexIndex * BYTES_PER_SLAB_VERTEX);

		headVertexIndex = newHeadVertexIndex;
		return true;
	}

	/** Returns true if successful, or false if capacity would be exceeded. */
	boolean transferFromSlab(ClusteredDrawableStorage region, Slab source) {
		assert RenderSystem.isOnRenderThread();
		final int newHeadVertexIndex = headVertexIndex + region.quadVertexCount;

		if (newHeadVertexIndex > allocator.maxSlabQuadVertexCount) {
			return false;
		}

		GFX.bindBuffer(GFX.GL_COPY_WRITE_BUFFER, glBufferId());
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, source.glBufferId());
		GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_COPY_WRITE_BUFFER, region.baseQuadVertexIndex() * BYTES_PER_SLAB_VERTEX, headVertexIndex * BYTES_PER_SLAB_VERTEX, region.byteCount);
		GFX.bindBuffer(GFX.GL_COPY_WRITE_BUFFER, 0);
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

		assert !allocatedRegions.contains(region);
		allocatedRegions.add(region);
		region.setLocation(this, headVertexIndex);
		addToVertexCounts(region.quadVertexCount);

		headVertexIndex = newHeadVertexIndex;
		return true;
	}

	void removeRegion(ClusteredDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();
		assert allocatedRegions.contains(region);
		assert !isClosed;

		if (allocatedRegions.remove(region)) {
			addToVertexCounts(-region.quadVertexCount);
		}
	}

	@Override
	public void bind() {
		assert !isClosed;

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			super.bind();
			TerrainFormat.TERRAIN_MATERIAL.enableAttributes();
			TerrainFormat.TERRAIN_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	@Override
	protected void onShutdown() {
		assert RenderSystem.isOnRenderThread();

		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}

		addToVertexCounts(-usedVertexCount);
		allocator.notifyShutdown(this);
	}
}
