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

import net.minecraft.util.math.MathHelper;

import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.varia.GFX;

//WIP: support direct-copy mapped transfer buffers when they are available
public class VertexCluster {
	//final StringBuilder log = new StringBuilder();

	private final VertexClusterRealm owner;
	private final ObjectArrayList<ClusteredDrawableStorage> noobs = new ObjectArrayList<>();
	private final ReferenceOpenHashSet<ClusteredDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();

	private int capacityBytes;
	private int glBufferId = 0;
	private boolean isClosed = false;

	private int headBytes = 0;
	private int newBytes = 0;
	private int vacantBytes = 0;

	final long clumpPos;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = 0;

	public VertexCluster(VertexClusterRealm owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	void close() {
		close(true);
	}

	void close(boolean notify) {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			clearVao();

			if (glBufferId != 0) {
				GlBufferAllocator.releaseBuffer(glBufferId, capacityBytes);
				glBufferId = 0;
			}

			headBytes = 0;
			newBytes = 0;
			vacantBytes = 0;

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
		}
	}

	public int capacityBytes() {
		return capacityBytes;
	}

	private void clearVao() {
		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}
	}

	public void bind() {
		assert glBufferId != 0 : "Vertex Storage Clump bound before upload";

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
			TerrainFormat.TERRAIN_MATERIAL.enableAttributes();
			TerrainFormat.TERRAIN_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
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

		if (glBufferId == 0) {
			uploadNewBuffer();
		} else if (headBytes + newBytes <= capacityBytes) {
			appendToBuffer();
		} else {
			recreateBuffer();
		}

		//log.append("Noobs cleared after upload").append("\n");
		noobs.clear();
		newBytes = 0;
	}

	private void uploadNewBuffer() {
		// never buffered, adjust capacity if needed before we create it
		assert headBytes == 0;
		capacityBytes = Math.max(capacityBytes, MathHelper.smallestEncompassingPowerOfTwo(newBytes));

		glBufferId = GlBufferAllocator.claimBuffer(capacityBytes);
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);
		appendNewRegionsAtHead();
	}

	private void appendToBuffer() {
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		appendNewRegionsAtHead();
	}

	private void appendNewRegionsAtHead() {
		assert capacityBytes - headBytes >= newBytes;

		int baseOffset = 0;

		for (ClusteredDrawableStorage noob : noobs) {
			final int byteCount = noob.byteCount;

			//log.append("Added to allocated regions: ").append(noob.id).append("\n");
			allocatedRegions.add(noob);
			noob.setBaseAddress(headBytes);
			noob.getAndClearTransferBuffer().releaseToBoundBuffer(GFX.GL_ARRAY_BUFFER, headBytes);
			baseOffset += byteCount;
			headBytes += byteCount;
		}

		assert baseOffset == newBytes;

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	private void recreateBuffer() {
		final int oldCapacityBytes = capacityBytes;
		capacityBytes = Math.max(capacityBytes, MathHelper.smallestEncompassingPowerOfTwo(headBytes - vacantBytes + newBytes));

		// bind existing buffer for read
		final int oldBufferId = glBufferId;
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, glBufferId);

		// create new buffer
		clearVao();
		glBufferId = GlBufferAllocator.claimBuffer(capacityBytes);
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);

		// Copy extant regions to new buffer
		// If no vacancies copy entire block, not by region
		if (vacantBytes == 0) {
			GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_ARRAY_BUFFER, 0, 0, headBytes);
		} else {
			headBytes = 0;

			// PERF: could be faster by copying contiguous blocks instead of by region
			for (ClusteredDrawableStorage region : allocatedRegions) {
				GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_ARRAY_BUFFER, region.baseByteAddress(), headBytes, region.byteCount);
				//assert isPresent(region);

				//log.append("Setting base address: ").append(region.id).append("\n");
				region.setBaseAddress(headBytes);
				headBytes += region.byteCount;
			}

			vacantBytes = 0;
		}

		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
		GlBufferAllocator.releaseBuffer(oldBufferId, oldCapacityBytes);

		// copy new regions to new buffer
		appendNewRegionsAtHead();
	}

	void notifyClosed(ClusteredDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		//log.append("Notify closed: ").append(region.id).append("\n");

		if (allocatedRegions.remove(region)) {
			vacantBytes += region.byteCount;
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
}
