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

package grondag.canvas.vf;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class VfImage<T extends VfElement<T>> {
	final AtomicInteger tail = new AtomicInteger();
	private final int intsPerElement;
	private final int bytesPerElement;
	private final Class<T> clazz;

	private int bufferId;
	protected volatile T[] elements;
	private int head = 0;
	private int imageCapacity;

	@SuppressWarnings("unchecked")
	public VfImage(int expectedCapacity, int intsPerElement, Class<T> clazz) {
		imageCapacity = expectedCapacity;
		this.clazz = clazz;
		elements = (T[]) Array.newInstance(clazz, expectedCapacity);
		this.intsPerElement = intsPerElement;
		bytesPerElement = intsPerElement * 4;
	}

	public void close() {
		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}
	}

	public synchronized void clear() {
		close();
		Arrays.fill(elements, null);
		head = 0;
		tail.set(0);
	}

	int bufferId() {
		return bufferId;
	}

	public void add(T element) {
		final int index = tail.getAndIncrement();
		element.setIndex(index);

		if (index > elements.length) {
			expand(MathHelper.smallestEncompassingPowerOfTwo(index));
		}

		elements[index] = element;

		//if ((index & 0xFF) == 0xFF) {
		//	System.out.println("vfUV count: " + index + " / " + VfInt.UV.count.get());
		//}
	}

	@SuppressWarnings("unchecked")
	protected synchronized void expand(int newCapacity) {
		if (newCapacity > elements.length) {
			VfElement<?>[] oldElements = elements;
			elements = (T[]) Array.newInstance(clazz, newCapacity);
			System.arraycopy(oldElements, 0, elements, 0, oldElements.length);
		}
	}

	public synchronized boolean upload() {
		final int tail = this.tail.get();
		final int len = tail - head;

		if (len == 0) {
			return false;
		}

		boolean didRecreate = false;
		final T[] elements = this.elements;

		if (bufferId == 0) {
			// never buffered, adjust capacity if needed before we create it
			imageCapacity = elements.length;

			bufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, imageCapacity * bytesPerElement, GFX.GL_STATIC_DRAW);
			didRecreate = true;
		} else if (elements.length > imageCapacity) {
			// have a buffer but it is too small
			imageCapacity = elements.length;

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
				T element = elements[i + head];
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
