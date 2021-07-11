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

package grondag.canvas.render.region.vs;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.util.math.BlockPos;

public class ClumpedVertexStorage {
	public static final ClumpedVertexStorage INSTANCE = new ClumpedVertexStorage();

	private static final int CLUMP_SHIFT = 2;
	private static final int BLOCKPOS_TO_CLUMP_SHIFT = 4 + CLUMP_SHIFT;

	static long clumpPos(long packedOriginBlockPos) {
		final int x = BlockPos.unpackLongX(packedOriginBlockPos);
		final int y = BlockPos.unpackLongY(packedOriginBlockPos);
		final int z = BlockPos.unpackLongZ(packedOriginBlockPos);
		return BlockPos.asLong(x >> BLOCKPOS_TO_CLUMP_SHIFT, y >> BLOCKPOS_TO_CLUMP_SHIFT, z >> BLOCKPOS_TO_CLUMP_SHIFT);
	}

	private final Long2ObjectOpenHashMap<ClumpedVertexStorageClump> clumps = new Long2ObjectOpenHashMap<>();
	private final ReferenceOpenHashSet<ClumpedVertexStorageClump> clumpUploads = new ReferenceOpenHashSet<>();

	private boolean isClosed = false;

	private ClumpedVertexStorage() { }

	public void clear() {
		assert RenderSystem.isOnRenderThread();

		for (ClumpedVertexStorageClump clump : clumps.values()) {
			clump.close();
		}

		clumps.clear();
		clumpUploads.clear();
	}

	public void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;
			clear();
		}
	}

	void allocate(ClumpedDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();

		ClumpedVertexStorageClump clump = clumps.computeIfAbsent(storage.clumpPos, p -> new ClumpedVertexStorageClump(ClumpedVertexStorage.this, p));
		clump.allocate(storage);
		clumpUploads.add(clump);
	}

	void notifyClosed(ClumpedVertexStorageClump clump) {
		assert RenderSystem.isOnRenderThread();
		ClumpedVertexStorageClump deadClump = clumps.remove(clump.clumpPos);
		assert deadClump != null : "Clump gone missing.";
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		if (!clumpUploads.isEmpty()) {
			for (ClumpedVertexStorageClump clump : clumpUploads) {
				clump.upload();
			}

			clumpUploads.clear();
		}
	}
}
