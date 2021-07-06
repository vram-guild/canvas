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

package grondag.canvas.vf.storage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class VfStorageImage<T extends VfStorageElement<T>> {
	private ObjectArrayList<T> elements = new ObjectArrayList<>();
	private final ObjectArrayList<T> addedElements = new ObjectArrayList<>();
	private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

	private int bufferId;
	private int headByteOffset = 0;
	private int imageCapacityBytes;
	boolean logging = false;

	public VfStorageImage(int imageCapacityBytes) {
		this.imageCapacityBytes = imageCapacityBytes;
	}

	public void close() {
		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}

		elements.clear();
		queue.clear();
		headByteOffset = 0;
	}

	public synchronized void clear() {
		close();
	}

	int bufferId() {
		return bufferId;
	}

	/**
	 * Thread-safe.
	 */
	public void enqueue(T element) {
		queue.add(element);
	}

	public synchronized boolean upload() {
		int addedByteCount = 0;

		T e = queue.poll();

		while (e != null) {
			addedElements.add(e);
			addedByteCount += e.byteSize();
			e = queue.poll();
		}

		if (addedElements.isEmpty()) {
			return false;
		}

		boolean didRecreate = false;

		if (bufferId == 0) {
			// never buffered, adjust capacity if needed before we create it
			imageCapacityBytes = Math.max(imageCapacityBytes, MathHelper.smallestEncompassingPowerOfTwo(addedByteCount));

			bufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, imageCapacityBytes, GFX.GL_STATIC_DRAW);
			didRecreate = true;
		} else if (headByteOffset + addedByteCount >= imageCapacityBytes) {
			// Have a buffer but it is too small so need to compact and maybe expand.
			// Note this leaves buffer bound for us.
			prepareForAddedBytes(addedByteCount);
			didRecreate = true;
		} else {
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
		}

		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_TEXTURE_BUFFER, headByteOffset, addedByteCount,
				GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

		if (bBuff == null) {
			CanvasMod.LOG.warn("Unable to map buffer. If this repeats, rendering will be incorrect and is probably a compatibility issue.");
		} else {
			final IntBuffer iBuff = bBuff.asIntBuffer();
			int intIndex = 0;

			for (T added : addedElements) {
				added.setByteAddress(headByteOffset);
				added.write(iBuff, intIndex);
				int bytes = added.byteSize();
				headByteOffset += bytes;
				intIndex += bytes / 4;
			}
		}

		GFX.flushMappedBufferRange(GFX.GL_TEXTURE_BUFFER, 0, addedByteCount);
		GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

		elements.addAll(addedElements);
		addedElements.clear();

		return didRecreate;
	}

	private void prepareForAddedBytes(int addedByteCount) {
		final ObjectArrayList<T> oldElements = this.elements;
		final ObjectArrayList<T> newElements = new ObjectArrayList<>();

		// see if we can compact
		int activeByteSize = 0;

		for (T e : oldElements) {
			if (!e.isClosed()) {
				activeByteSize += e.byteSize();
				newElements.add(e);
			}
		}

		elements = newElements;

		// if compacting will make us more than half full then double the image size
		// or if that is insufficient, expand to fit new content
		final int requiredByteSize = activeByteSize + addedByteCount;

		if (requiredByteSize > imageCapacityBytes / 2) {
			imageCapacityBytes = Math.max(imageCapacityBytes * 2, MathHelper.smallestEncompassingPowerOfTwo(requiredByteSize));
		}

		final int newBufferId = GFX.genBuffer();
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, newBufferId);
		GFX.bufferData(GFX.GL_TEXTURE_BUFFER, imageCapacityBytes, GFX.GL_STATIC_DRAW);
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, bufferId);

		headByteOffset = 0;

		for (T e : newElements) {
			GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_TEXTURE_BUFFER, e.getByteAddress(), headByteOffset, e.byteSize());
			e.setByteAddress(headByteOffset);
			headByteOffset += e.byteSize();
		}

		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

		GFX.deleteBuffers(bufferId);
		bufferId = newBufferId;
	}
}
