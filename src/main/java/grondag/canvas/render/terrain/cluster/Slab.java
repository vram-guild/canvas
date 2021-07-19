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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.varia.GFX;

class Slab extends AbstractGlBuffer {
	private SlabIndex slabIndex = new SlabIndex();
	private int headVertexIndex = 0;
	private boolean isClaimed;
	private int vaoBufferId = 0;
	private final ReferenceOpenHashSet<ClusteredDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();
	private int usedVertexCount;

	private Slab() {
		super(BYTES_PER_SLAB, GFX.GL_ARRAY_BUFFER, GFX.GL_STREAM_DRAW);
	}

	/** How much vertex capacity is remaining. */
	int availableVertexCount() {
		return MAX_QUAD_VERTEX_COUNT - headVertexIndex;
	}

	boolean isEmpty() {
		return usedVertexCount == 0;
	}

	Set<ClusteredDrawableStorage> regions() {
		return allocatedRegions;
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		assert allocatedRegions.isEmpty();
		headVertexIndex = 0;
		isClaimed = false;
		POOL.offer(this);
	}

	/** Returns true if successful, or false if capacity would be exceeded. */
	boolean allocateAndLoad(ClusteredDrawableStorage region) {
		final int newHeadVertexIndex = headVertexIndex + region.quadVertexCount;

		if (newHeadVertexIndex > MAX_QUAD_VERTEX_COUNT) {
			return false;
		}

		assert !allocatedRegions.contains(region);
		allocatedRegions.add(region);
		region.setLocation(this, headVertexIndex);
		usedVertexCount += region.quadVertexCount;

		GFX.bindBuffer(bindTarget, glBufferId());
		region.getAndClearTransferBuffer().releaseToBoundBuffer(GFX.GL_ARRAY_BUFFER, headVertexIndex * BYTES_PER_VERTEX);

		headVertexIndex = newHeadVertexIndex;
		return true;
	}

	void removeRegion(ClusteredDrawableStorage region) {
		assert allocatedRegions.contains(region);

		if (allocatedRegions.remove(region)) {
			usedVertexCount -= region.quadVertexCount;
		}
	}

	@Override
	public void bind() {
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

		slabIndex.bind();
	}

	@Override
	protected void onShutdown() {
		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}

		slabIndex.shutdown();
		slabIndex = null;
	}

	class SlabIndex extends AbstractGlBuffer {
		protected SlabIndex() {
			super(BYTES_PER_SLAB_INDEX, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);

			// WIP: lol
			GFX.bindBuffer(bindTarget, glBufferId());
			final ByteBuffer buff = GFX.mapBuffer(bindTarget, GFX.GL_WRITE_ONLY);
			final ShortBuffer sink = buff.asShortBuffer();
			int i = 0;
			int v = 0;

			while (i < MAX_TRI_VERTEX_COUNT) {
				sink.put(i++, (short) v);
				sink.put(i++, (short) (v + 1));
				sink.put(i++, (short) (v + 2));
				sink.put(i++, (short) (v + 2));
				sink.put(i++, (short) (v + 3));
				sink.put(i++, (short) v);
				v += 4;
			}

			GFX.unmapBuffer(bindTarget);
		}

		@Override
		protected void onShutdown() {
			// NOOP
		}
	}

	/** We are using short index arrays, which means we can't have more than this many vertices per slab. */
	private static final int MAX_QUAD_VERTEX_COUNT = 0x10000;
	private static final int MAX_TRI_VERTEX_COUNT = MAX_QUAD_VERTEX_COUNT * 6 / 4;
	private static final int BYTES_PER_VERTEX = 28;
	private static final int BYTES_PER_SLAB = (MAX_QUAD_VERTEX_COUNT * BYTES_PER_VERTEX);

	/** Buffer size needed to index an entire slab with triangle verticies.  Is * 2 because 2bytes per index. */
	private static final int BYTES_PER_SLAB_INDEX = MAX_TRI_VERTEX_COUNT * 2;

	private static final ArrayDeque<Slab> POOL = new ArrayDeque<>();
	private static int totalSlabCount = 0;

	static {
		// Want IDE to show actual numbers above, so check here at run time that nothing changed and got missed.
		assert BYTES_PER_VERTEX == TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes : "Slab vertex size doesn't match vertex format";
	}

	static Slab claim() {
		assert RenderSystem.isOnRenderThread();

		Slab result = POOL.poll();

		if (result == null) {
			result = new Slab();
			++totalSlabCount;
		}

		result.isClaimed = true;
		return result;
	}

	static String debugSummary() {
		return String.format("slabs: %dMb / %dMb", (totalSlabCount - POOL.size()) * BYTES_PER_SLAB / 0x100000, totalSlabCount * BYTES_PER_SLAB / 0x100000);
	}
}
