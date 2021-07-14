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
import java.util.Comparator;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.CanvasMod;
import grondag.canvas.varia.GFX;

//WIP: support direct-copy mapped transfer buffers when they are available
public class ClumpedVertexStorageClump {
	private static final int NO_BUFFER = -1;

	//final StringBuilder log = new StringBuilder();

	private final ClumpedVertexStorage owner;
	private final ObjectArrayList<ClumpedDrawableStorage> noobs = new ObjectArrayList<>();
	private final ReferenceOpenHashSet<ClumpedDrawableStorage> allocatedRegions = new ReferenceOpenHashSet<>();
	private LongArrayList vacancies = new LongArrayList();

	private int capacityBytes;
	private int glBufferId = NO_BUFFER;
	private boolean isClosed = false;

	private int headBytes = 0;
	private int newBytes = 0;
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
		newBytes = 0;
		vacantBytes = 0;

		assert areAllRegionsClosed();
		noobs.clear();
		//log.append("Clear").append("\n");
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

		//log.append("Added to noobs: ").append(storage.id).append("\n");
		noobs.add(storage);
		storage.setClump(this);
		newBytes += storage.byteCount;

		//assert isPresent(storage);
	}

	/** For assertion checks only. */
	boolean isPresent(ClumpedDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();
		return allocatedRegions.contains(storage) || noobs.contains(storage);
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		if (newBytes == 0) return;

		assert !noobs.isEmpty();

		if (glBufferId == NO_BUFFER) {
			uploadNewBuffer();
		} else if (headBytes + newBytes <= capacityBytes) {
			appendToBuffer();
		} else if (!loadNewRegionsToVacancies()) {
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

		glBufferId = GFX.genBuffer();
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);
		appendNewRegionsAtHead();
	}

	private void appendToBuffer() {
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		appendNewRegionsAtHead();
	}

	private static final Comparator<ClumpedDrawableStorage> BYTE_SIZE_INVERSE_COMPARATOR = (o1, o2) -> {
		// works because size is packed in high bits
		return Integer.compare(o2.byteCount, o1.byteCount);
	};

	private boolean loadSingleRegionToVacancy() {
		long vacancy = vacancies.getLong(0);
		assert vacantBytes == unpackVacancyBytes(vacancy);
		assert noobs.size() == 1;
		assert vacancies.size() == 1;

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		loadRegionToVacancy(noobs.get(0), vacancy);
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);

		vacancies.clear();
		vacantBytes = 0;
		return true;
	}

	/** Assumes buffer already bound. */
	private void loadRegionToVacancy(ClumpedDrawableStorage region, long vacancy) {
		int baseAddress = unpackVacancyAddress(vacancy);

		//log.append("Added to allocated regions: ").append(region.id).append("\n");
		allocatedRegions.add(region);
		region.getAndClearTransferBuffer().releaseToSubBuffer(GFX.GL_ARRAY_BUFFER, baseAddress, region.byteCount);
		region.setBaseAddress(baseAddress);

		int remaining = unpackVacancyBytes(vacancy) - region.byteCount;
		assert remaining >= 0;
		region.paddingBytes = remaining;
	}

	// WIP: handle case when can slot some into vacancies and remaining empty is enough for the rest

	private boolean loadNewRegionsToVacancies() {
		// Currently only handle the case when number of new regions <= number of vacancies.
		// If we get a whole new region the chance of fitting is probably much smaller.
		if (newBytes > vacantBytes || vacancies.size() < noobs.size()) {
			return false;
		}

		if (noobs.size() == 1 && vacancies.size() == 1) {
			return loadSingleRegionToVacancy();
		}

		// vacancies are smallest to largest
		// regions are largest to smallest
		vacancies.sort(LongComparators.NATURAL_COMPARATOR);
		noobs.sort(BYTE_SIZE_INVERSE_COMPARATOR);

		final int limit = noobs.size();
		int vacancyIndex = 0;

		for (var noob : noobs) {
			while (vacancyIndex < limit) {
				// find smallest vacancy that will hold it
				if (noob.byteCount <= unpackVacancyBytes(vacancies.getLong(vacancyIndex++))) {
					break;
				}
			}

			if (vacancyIndex == limit) {
				return false;
			}
		}

		// If we got to here then we can fit, so we repeat for real this time
		vacancyIndex = 0;
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
		final LongArrayList newVacancies = new LongArrayList();

		for (var noob : noobs) {
			while (vacancyIndex < limit) {
				// find smallest vacancy that will hold it
				final long vacancy = vacancies.getLong(vacancyIndex++);
				final int byteCount = unpackVacancyBytes(vacancy);

				if (noob.byteCount <= byteCount) {
					vacantBytes -= byteCount;
					loadRegionToVacancy(noob, vacancy);
					break;
				} else {
					newVacancies.add(vacancy);
				}
			}
		}

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);

		// preserve unused vacancies
		while (vacancyIndex < limit) {
			newVacancies.add(vacancies.getLong(vacancyIndex++));
		}

		assert vacantBytes >= 0;
		vacancies = newVacancies;
		return true;
	}

	private void appendNewRegionsAtHead() {
		assert capacityBytes - headBytes >= newBytes;

		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_ARRAY_BUFFER, headBytes, newBytes,
				GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

		int baseOffset = 0;

		if (bBuff == null) {
			CanvasMod.LOG.warn("Unable to map buffer. If this repeats, rendering will be incorrect and is probably a compatibility issue.");
		} else {
			for (ClumpedDrawableStorage noob : noobs) {
				final int byteCount = noob.byteCount;

				//log.append("Added to allocated regions: ").append(noob.id).append("\n");
				allocatedRegions.add(noob);
				noob.setBaseAddress(headBytes);
				noob.getAndClearTransferBuffer().releaseToMappedBuffer(bBuff, baseOffset, 0, byteCount);
				baseOffset += byteCount;
				headBytes += byteCount;
			}

			assert baseOffset == newBytes;

			GFX.flushMappedBufferRange(GFX.GL_ARRAY_BUFFER, 0, baseOffset);
			GFX.unmapBuffer(GFX.GL_ARRAY_BUFFER);
		}

		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
	}

	private void recreateBuffer() {
		capacityBytes = Math.max(capacityBytes, MathHelper.smallestEncompassingPowerOfTwo(headBytes - vacantBytes + newBytes));

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

			// PERF: could be faster by copying contiguous blocks instead of by region
			for (ClumpedDrawableStorage region : allocatedRegions) {
				GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_ARRAY_BUFFER, region.baseByteAddress(), headBytes, region.byteCount);
				//assert isPresent(region);

				//log.append("Setting base address: ").append(region.id).append("\n");
				region.setBaseAddress(headBytes);
				headBytes += region.byteCount;
			}

			vacantBytes = 0;
			vacancies.clear();
		}

		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

		// copy new regions to new buffer
		appendNewRegionsAtHead();
	}

	void notifyClosed(ClumpedDrawableStorage region) {
		assert RenderSystem.isOnRenderThread();

		//log.append("Notify closed: ").append(region.id).append("\n");

		if (allocatedRegions.remove(region)) {
			int byteCount = region.byteCount + region.paddingBytes;
			vacancies.add(packVacancy(byteCount, region.baseByteAddress()));
			vacantBytes += byteCount;
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

	private static long packVacancy(int byteCount, int baseAddress) {
		return ((long) byteCount << 32) | baseAddress;
	}

	private static int unpackVacancyBytes(long packedVacancy) {
		return (int) (packedVacancy >>> 32);
	}

	private static int unpackVacancyAddress(long packedVacancy) {
		return (int) (packedVacancy & 0xFFFFFFFF);
	}
}
