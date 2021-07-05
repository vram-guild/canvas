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

package grondag.canvas.vf.index;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class VfIndexImage<T extends VfIndexElement<T>> {
	private static final int PAGE_SIZE = 0x10000;

	final AtomicInteger tail = new AtomicInteger();
	final AtomicInteger lastPageIndex = new AtomicInteger();

	private final int intsPerElement;
	private final int bytesPerElement;

	private int bufferId;
	protected Object[][] elements = new Object[4096][];
	private int head = 0;
	private int imageCapacity;
	boolean logging = false;

	public VfIndexImage(int intsPerElement) {
		elements[0] = new Object[PAGE_SIZE];
		imageCapacity = PAGE_SIZE;
		this.intsPerElement = intsPerElement;
		bytesPerElement = intsPerElement * 4;
	}

	public void close() {
		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}

		Arrays.fill(elements, null);
	}

	public synchronized void clear() {
		close();
		Arrays.fill(elements, null);
		elements[0] = new Object[PAGE_SIZE];
		head = 0;
		tail.set(0);
		lastPageIndex.set(0);
	}

	int bufferId() {
		return bufferId;
	}

	private Object[] getOrCreatePage(final int pageIndex) {
		Object[] result = elements[pageIndex];
		int tryCount = 0;

		while (result == null) {
			assert checkTryCount(++tryCount);

			final int lastPage = lastPageIndex.get();

			if (lastPage < pageIndex) {
				final int newPage = lastPage + 1;

				if (lastPageIndex.compareAndSet(lastPage, newPage)) {
					elements[newPage] = new Object[PAGE_SIZE];
				}
			}

			result = elements[pageIndex];
		}

		return result;
	}

	/** Always true. Done this way to fully shortcut when assertions are off. */
	private boolean checkTryCount(final int tryCount) {
		if ((tryCount & 0xF) == 0xF) {
			CanvasMod.LOG.info("Excessive retries in buffer texture page acquisition: " + tryCount);
		}

		return true;
	}

	/**
	 * Thread-safe.
	 */
	public void add(T element) {
		final int index = tail.getAndIncrement();
		element.setIndex(index);
		getOrCreatePage(index >> 16)[index & 0xFFFF] = element;

		//if (logging && (index & 0xFF) == 0xFF) {
		//	System.out.println("vfLight count: " + index + " / " + VfInt.LIGHT.count.get());
		//}
	}

	public synchronized boolean upload() {
		final int tail = this.tail.get();
		int limit = head;

		// could have threads waiting on new pages or not yet written so tail can overrun our array
		for (; limit < tail; ++limit) {
			final Object[] page = elements[limit >> 16];

			if (page == null || page[limit & 0xFFFF] == null) {
				break;
			}
		}

		final int len = limit - head;

		assert len >= 0;

		if (len == 0) {
			return false;
		}

		boolean didRecreate = false;

		if (bufferId == 0) {
			// never buffered, adjust capacity if needed before we create it
			imageCapacity = (limit & 0xFFFF0000) + PAGE_SIZE;

			bufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, imageCapacity * bytesPerElement, GFX.GL_STATIC_DRAW);
			didRecreate = true;
		} else if (limit >= imageCapacity) {
			// have a buffer but it is too small
			imageCapacity = (limit & 0xFFFF0000) + PAGE_SIZE;

			final int newBufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, newBufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, imageCapacity * bytesPerElement, GFX.GL_STATIC_DRAW);

			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, bufferId);
			GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_TEXTURE_BUFFER, 0, 0, head * bytesPerElement);
			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

			GFX.deleteBuffers(bufferId);
			bufferId = newBufferId;

			didRecreate = true;
		} else {
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
		}

		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_TEXTURE_BUFFER, head * bytesPerElement, len * bytesPerElement,
				GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

		if (bBuff != null) {
			final IntBuffer iBuff = bBuff.asIntBuffer();

			for (int i = 0; i < len; ++i) {
				final int index = i + head;
				@SuppressWarnings("unchecked")
				final T element = (T) elements[index >> 16][index & 0xFFFF];
				element.write(iBuff, i * intsPerElement);
			}

			head = tail;
		}

		GFX.flushMappedBufferRange(GFX.GL_TEXTURE_BUFFER, 0, len * bytesPerElement);
		GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

		return didRecreate;
	}
}
