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

package grondag.canvas.buffer.encoding;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.math.MathHelper;

public abstract class AbstractVertexArray implements VertexBufferAccess {
	protected int capacity = 1024;
	protected int[] vertexData = new int[capacity];
	/** also the index of the first vertex when used in VertexConsumer mode. */
	protected int integerSize = 0;
	protected int currentVertexIndex = 0;

	public AbstractVertexArray() {
		arrayCount.incrementAndGet();
		arryBytes.addAndGet(capacity);
	}

	protected void grow(int newSize) {
		if (newSize > capacity) {
			final int newCapacity = MathHelper.smallestEncompassingPowerOfTwo(newSize);
			final int[] newData = new int[newCapacity];
			System.arraycopy(vertexData, 0, newData, 0, capacity);
			arryBytes.addAndGet(newCapacity - capacity);
			capacity = newCapacity;
			vertexData = newData;
		}
	}

	public int integerSize() {
		return integerSize;
	}

	public int byteSize() {
		return integerSize * 4;
	}

	public boolean isEmpty() {
		return integerSize == 0;
	}

	static AtomicInteger arrayCount = new AtomicInteger();
	static AtomicInteger arryBytes = new AtomicInteger();

	public static String debugReport() {
		return String.format("CPU Vertex Arrays - count;%d,   MB allocated:%f", arrayCount.get(), arryBytes.get() / 1048576f);
	}

	@Override
	public int[] data() {
		return vertexData;
	}

	@Override
	public int allocate(int size) {
		final int result = integerSize;
		final int newSize = result + size;
		grow(newSize);
		integerSize = newSize;
		return result;
	}
}
