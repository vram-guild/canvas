/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffer.packing;

import java.util.function.Consumer;

import grondag.canvas.material.old.MaterialState;

/**
 * Tracks number of vertices, pipeline and sequence thereof within a buffer.
 */
public class BufferPackingList {
	private int[] starts = new int[16];
	private int[] counts = new int[16];
	private MaterialState[] materialStates = new MaterialState[16];

	private int size = 0;
	private int totalBytes = 0;

	public BufferPackingList() {

	}

	public void clear() {
		size = 0;
		totalBytes = 0;
	}

	public int size() {
		return size;
	}

	/**
	 * For performance testing.
	 */
	public int quadCount() {
		if (size == 0) {
			return 0;
		}

		int quads = 0;

		for (int i = 0; i < size; i++) {
			quads += counts[i] / 4;
		}
		return quads;
	}

	public int totalBytes() {
		return totalBytes;
	}

	public void addPacking(MaterialState materialState, int startVertex, int vertexCount) {
		if (size == materialStates.length) {
			final int cCopy[] = new int[size * 2];
			System.arraycopy(counts, 0, cCopy, 0, size);
			counts = cCopy;

			final int sCopy[] = new int[size * 2];
			System.arraycopy(starts, 0, sCopy, 0, size);
			starts = sCopy;

			final MaterialState pCopy[] = new MaterialState[size * 2];
			System.arraycopy(materialStates, 0, pCopy, 0, size);
			materialStates = pCopy;
		}

		materialStates[size] = materialState;
		starts[size] = startVertex;
		counts[size] = vertexCount;
		totalBytes += materialState.materialVertexFormat().vertexStrideBytes * vertexCount;
		size++;
	}

	public final void forEach(BufferPacker consumer) {
		final int size = this.size;
		for (int i = 0; i < size; i++) {
			consumer.accept(materialStates[i], starts[i], counts[i]);
		}
	}

	public final void forEachMaterialState(Consumer<MaterialState> consumer) {
		final int size = this.size;
		for (int i = 0; i < size; i++) {
			consumer.accept(materialStates[i]);
		}
	}

	public final MaterialState getMaterialState(int index) {
		return materialStates[index];
	}

	public final int getCount(int index) {
		return counts[index];
	}

	public final int getStart(int index) {
		return starts[index];
	}
}
