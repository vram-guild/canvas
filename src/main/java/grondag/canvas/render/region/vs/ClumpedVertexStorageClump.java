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

import java.nio.ByteBuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.varia.GFX;

public class ClumpedVertexStorageClump {
	private static final int NO_BUFFER = -1;

	private final ClumpedVertexStorage owner;
	private final ObjectArrayList<ClumpedDrawableStorage> noobs = new ObjectArrayList<>();
	private final ReferenceOpenHashSet<ClumpedDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();
	private final LongArrayList vacancies = new LongArrayList();

	private int capacityBytes;
	private int glBufferId = NO_BUFFER;
	private boolean isClosed = false;

	private int headBytes = 0;
	private int tailBytes = 0;
	private int vacantBytes = 0;

	final long clumpPos;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = NO_BUFFER;

	public ClumpedVertexStorageClump(ClumpedVertexStorage owner, long clumpPos) {
		this.owner = owner;
		this.clumpPos = clumpPos;
	}

	private void clear() {
		assert RenderSystem.isOnRenderThread();
		headBytes = 0;
		tailBytes = 0;
		vacantBytes = 0;

		assert areAllRegionsClosed();
		noobs.clear();
		allocatedRegions.clear();
		vacancies.clear();
	}

	private boolean areAllRegionsClosed() {
		// Note that we don't call close from here - that's controlled by drawlists/regions.
		// But active stores referencing this chunk shouldn't be a thing, otherwise might
		// try to draw with it.

		for (ClumpedDrawableStorage region : allocatedRegions) {
			if (!region.isClosed()) {
				return false;
			}
		}

		return true;
	}

	void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			clearVao();

			if (glBufferId != NO_BUFFER) {
				GFX.deleteBuffers(glBufferId);
				glBufferId = NO_BUFFER;
			}

			clear();
		}
	}

	private void clearVao() {
		if (vaoBufferId != NO_BUFFER) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = NO_BUFFER;
		}
	}

	public void bind() {
		assert glBufferId != NO_BUFFER : "Vertex Storage Clump bound before upload";

		if (vaoBufferId == NO_BUFFER) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
			VsFormat.VS_MATERIAL.enableAttributes();
			VsFormat.VS_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	void allocate(ClumpedDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();

		storage.setClump(this);
		noobs.add(storage);
		tailBytes += storage.byteCount;
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		assert tailBytes >= headBytes;

		if (headBytes == tailBytes) return;

		assert !noobs.isEmpty();

		// WIP: handle reuse of vacancies

		if (glBufferId == NO_BUFFER) {
			uploadNewBuffer();
		} else if (tailBytes <= capacityBytes) {
			appendToBuffer();
		} else {
			recreateBuffer();
		}
	}

	private void uploadNewBuffer() {
		// never buffered, adjust capacity if needed before we create it
		capacityBytes = Math.max(capacityBytes, MathHelper.smallestEncompassingPowerOfTwo(tailBytes));

		glBufferId = GFX.genBuffer();
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);
		appendNewRegionsAtHead();
	}

	private void appendToBuffer() {
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		appendNewRegionsAtHead();
	}

	private void appendNewRegionsAtHead() {
		final int lenBytes = tailBytes - headBytes;
		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_ARRAY_BUFFER, headBytes, lenBytes,
				GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

		int baseOffset = 0;

		if (bBuff == null) {
			CanvasMod.LOG.warn("Unable to map buffer. If this repeats, rendering will be incorrect and is probably a compatibility issue.");
		} else {
			for (ClumpedDrawableStorage noob : noobs) {
				final ByteBuffer transferBuffer = noob.getAndClearTransferBuffer();
				final int byteCount = noob.byteCount;

				allocatedRegions.add(noob);
				noob.setBaseVertex(headBytes / VsFormat.VS_MATERIAL.vertexStrideBytes);
				bBuff.put(baseOffset, transferBuffer, 0, byteCount);
				TransferBufferAllocator.release(transferBuffer);
				baseOffset += byteCount;
				headBytes += byteCount;
			}

			noobs.clear();

			assert headBytes == tailBytes;

			GFX.flushMappedBufferRange(GFX.GL_ARRAY_BUFFER, 0, lenBytes);
			GFX.unmapBuffer(GFX.GL_ARRAY_BUFFER);
		}

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	private void recreateBuffer() {
		tailBytes = tailBytes - vacantBytes;
		capacityBytes = Math.max(capacityBytes, MathHelper.smallestEncompassingPowerOfTwo(tailBytes));

		// bind existing buffer for read
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, glBufferId);

		// create new buffer
		clearVao();
		glBufferId = GFX.genBuffer();
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);

		// Copy extant regions to new buffer
		// If no vacancies copy entire block, not by region
		if (vacantBytes == 0) {
			GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_ARRAY_BUFFER, 0, 0, headBytes);
		} else {
			headBytes = 0;
			vacantBytes = 0;
			vacancies.clear();

			// PERF: could be faster by copying contiguous blocks instead of by region
			for (ClumpedDrawableStorage region : allocatedRegions) {
				GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_ARRAY_BUFFER, region.baseByteAddress(), headBytes, region.byteCount);
				region.setBaseVertex(headBytes / VsFormat.VS_MATERIAL.vertexStrideBytes);
				headBytes += region.byteCount;
			}
		}

		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

		// copy new regions to new buffer
		appendNewRegionsAtHead();
	}

	void notifyClosed(ClumpedDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		final boolean found = allocatedRegions.remove(region);
		assert found : "Closed clump region not found in clump";

		if (allocatedRegions.isEmpty()) {
			close();
			owner.notifyClosed(this);
		} else {
			vacancies.add((region.baseByteAddress() << 32) | region.byteCount);
		}
	}
}
