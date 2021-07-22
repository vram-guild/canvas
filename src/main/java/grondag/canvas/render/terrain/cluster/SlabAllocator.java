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

import java.util.ArrayDeque;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.render.terrain.TerrainFormat;

public class SlabAllocator {
	private final ArrayDeque<Slab> POOL = new ArrayDeque<>();

	private int totalSlabCount = 0;
	private int totalUsedVertexCount = 0;
	final int maxSlabQuadVertexCount;
	final int bytesPerSlab;

	private SlabAllocator(int maxSlabQuadVertexCount) {
		this.maxSlabQuadVertexCount = maxSlabQuadVertexCount;
		bytesPerSlab = maxSlabQuadVertexCount * BYTES_PER_SLAB_VERTEX;
	}

	Slab claim(int slot) {
		assert RenderSystem.isOnRenderThread();

		Slab result = POOL.poll();

		if (result == null) {
			result = new Slab(this);
			++totalSlabCount;
		} else {
			result.orphan();
		}

		result.prepareForClaim(slot);
		return result;
	}

	void addToVertexCount(int vertexCount) {
		totalUsedVertexCount += vertexCount;
	}

	void release(Slab slab) {
		POOL.offer(slab);
	}

	void notifyShutdown(Slab slab) {
		--totalSlabCount;
	}

	/** We are using short index arrays, which means we can't have more than this many quad vertices per slab. */
	public static final int MAX_SLAB_QUAD_VERTEX_COUNT = 0x10000;
	static final int BYTES_PER_SLAB_VERTEX = 28;

	static {
		// Want IDE to show actual numbers above, so check here at run time that nothing changed and got missed.
		assert BYTES_PER_SLAB_VERTEX == TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes : "Slab vertex size doesn't match vertex format";
	}

	private static final SlabAllocator LARGE = new SlabAllocator(MAX_SLAB_QUAD_VERTEX_COUNT);
	private static final SlabAllocator SMALL = new SlabAllocator(MAX_SLAB_QUAD_VERTEX_COUNT / 2);
	private static final int LARGE_BYTE_THRESHOLD = SMALL.bytesPerSlab * 3;

	static Slab claim(int currentBytes, int newBytes, int slot) {
		return newBytes > SMALL.bytesPerSlab || (currentBytes + currentBytes) >= LARGE_BYTE_THRESHOLD ? LARGE.claim(slot) : SMALL.claim(slot);
	}

	static int expectedBytesForNewSlab(int activeBytes) {
		return activeBytes >= LARGE_BYTE_THRESHOLD ? LARGE.bytesPerSlab : SMALL.bytesPerSlab;
	}

	public static String debugSummary() {
		return String.format("Slabs:%dMb/%dMb occ:%d",
				((LARGE.totalSlabCount - LARGE.POOL.size()) * LARGE.bytesPerSlab + (SMALL.totalSlabCount - SMALL.POOL.size()) * SMALL.bytesPerSlab) / 0x100000,
				(LARGE.totalSlabCount * LARGE.bytesPerSlab + SMALL.totalSlabCount * SMALL.bytesPerSlab) / 0x100000,
				((LARGE.totalUsedVertexCount + SMALL.totalUsedVertexCount) * 100L) / (LARGE.totalSlabCount * LARGE.maxSlabQuadVertexCount + SMALL.totalSlabCount * SMALL.maxSlabQuadVertexCount));
	}
}
