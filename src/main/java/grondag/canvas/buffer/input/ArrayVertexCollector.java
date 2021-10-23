/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.Mth;

import grondag.canvas.buffer.render.TransferBuffer;

public abstract class ArrayVertexCollector implements DrawableVertexCollector {
	protected final int quadStrideInts;
	protected int capacity = 1024;
	protected int[] vertexData = new int[capacity];

	/** also the index of the first vertex when used in VertexConsumer mode. */
	protected int integerSize = 0;

	public ArrayVertexCollector(int quadStrideInts) {
		this.quadStrideInts = quadStrideInts;
		arrayCount.incrementAndGet();
		arryBytes.addAndGet(capacity);
	}

	protected final void grow(int newSize) {
		final int oldCapacity = capacity;

		if (newSize > oldCapacity) {
			final int newCapacity = Mth.smallestEncompassingPowerOfTwo(newSize);
			final int[] newData = new int[newCapacity];
			System.arraycopy(vertexData, 0, newData, 0, oldCapacity);
			arryBytes.addAndGet(newCapacity - oldCapacity);
			capacity = newCapacity;
			vertexData = newData;
		}
	}

	public final int integerSize() {
		return integerSize;
	}

	@Override
	public final int byteSize() {
		return integerSize * 4;
	}

	@Override
	public final int quadCount() {
		return integerSize / quadStrideInts;
	}

	public final boolean isEmpty() {
		return integerSize == 0;
	}

	@Override
	public final int[] data() {
		return vertexData;
	}

	@Override
	public final int allocate(int size) {
		final int result = integerSize;
		final int newSize = result + size;
		grow(newSize);
		integerSize = newSize;
		return result;
	}

	@Override
	public final void toBuffer(IntBuffer intBuffer, int startingIndex) {
		intBuffer.put(vertexData, startingIndex, integerSize);
	}

	public final void toBuffer(int collectorSourceIndex, TransferBuffer targetBuffer, int bufferTargetIndex) {
		targetBuffer.put(vertexData, collectorSourceIndex, bufferTargetIndex, integerSize);
	}

	@Override
	public void clear() {
		integerSize = 0;
	}

	public final int[] saveState(int[] priorState) {
		final int integerSize = this.integerSize;

		if (integerSize == 0) {
			return null;
		}

		int[] result = priorState;

		if (result == null || result.length != integerSize) {
			result = new int[integerSize];
		}

		if (integerSize > 0) {
			System.arraycopy(vertexData, 0, result, 0, integerSize);
		}

		return result;
	}

	public final void loadState(int[] stateData) {
		clear();

		if (stateData != null) {
			final int size = stateData.length;
			allocate(size);
			System.arraycopy(stateData, 0, vertexData, 0, size);
		}
	}

	private static AtomicInteger arrayCount = new AtomicInteger();
	private static AtomicInteger arryBytes = new AtomicInteger();

	public static String debugReport() {
		return String.format("Vertex collectors: %d %4.1fMb", arrayCount.get(), arryBytes.get() / 1048576f);
	}
}
