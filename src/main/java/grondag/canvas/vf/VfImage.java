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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public final class VfImage {
	private int bufferId;
	private volatile int capacity = 0x10000;
	private int head = 0;
	private final AtomicInteger tail = new AtomicInteger();
	private final ConcurrentLinkedQueue<VfElement> QUEUE = new ConcurrentLinkedQueue<>();

	public void close() {
		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}
	}

	int bufferId() {
		return bufferId;
	}

	public void add(VfElement element) {
		element.setIndex(tail.getAndAdd(element.length()));
		QUEUE.add(element);
	}

	public synchronized boolean upload() {
		final int tail = this.tail.get();
		int len = tail - head;

		if (len == 0) {
			return false;
		}

		boolean didRecreate = false;

		if (bufferId == 0) {
			// never buffered, adjust capacity if needed before we create it
			if (tail > capacity) {
				capacity = MathHelper.smallestEncompassingPowerOfTwo(tail);
			}

			bufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, capacity * 4, GFX.GL_STATIC_DRAW);
			didRecreate = true;
		} else if (tail > capacity) {
			// have a buffer but it is too small
			capacity = MathHelper.smallestEncompassingPowerOfTwo(tail);

			final int newBufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, newBufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, capacity * 4, GFX.GL_STATIC_DRAW);

			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, bufferId);
			GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, GFX.GL_TEXTURE_BUFFER, 0, 0, head);
			GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);

			GFX.deleteBuffers(bufferId);
			bufferId = newBufferId;

			didRecreate = true;
		} else {
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
		}

		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_TEXTURE_BUFFER, head, len, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT);

		if (bBuff != null) {
			final IntBuffer iBuff = bBuff.asIntBuffer();
			VfElement element = QUEUE.poll();

			while (element != null) {
				element.write(iBuff);
				element = QUEUE.poll();
			}

			head = tail;
		}

		GFX.flushMappedBufferRange(GFX.GL_TEXTURE_BUFFER, 0, len);
		GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);

		return didRecreate;
	}
}
