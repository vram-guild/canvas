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

import grondag.canvas.render.terrain.TerrainFormat;

public class SlabAllocator {
	private static int slabCount = 0;
	private static long usedBytes = 0;
	private static long capacityBytes = 0;

	static void addToVertexCount(int vertexCount) {
		usedBytes += vertexCount * BYTES_PER_SLAB_VERTEX;
	}

	static void notifyShutdown(Slab slab) {
		assert slab.usedVertexCount() == 0;
		--slabCount;
		capacityBytes -= slab.capacityBytes();
	}

	public static final int SLAB_QUAD_VERTEX_COUNT_INCREMENT = 0x2000;
	public static final int BYTES_PER_SLAB_VERTEX = 28;
	static final int SLAB_BYTES_INCREMENT = SLAB_QUAD_VERTEX_COUNT_INCREMENT * BYTES_PER_SLAB_VERTEX;

	static {
		// Want IDE to show actual numbers above, so check here at run time that nothing changed and got missed.
		assert BYTES_PER_SLAB_VERTEX == TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes : "Slab vertex size doesn't match vertex format";
	}

	static Slab claim(int minCapacityBytes) {
		assert RenderSystem.isOnRenderThread();
		++slabCount;
		final var result = new Slab((minCapacityBytes + SLAB_BYTES_INCREMENT - 1) / SLAB_BYTES_INCREMENT * SLAB_BYTES_INCREMENT);
		capacityBytes += result.capacityBytes();
		return result;
	}

	public static String debugSummary() {
		return String.format("%d slabs %dMb occ:%d",
				slabCount,
				capacityBytes / 0x100000L,
				usedBytes * 100L / capacityBytes);
	}
}
