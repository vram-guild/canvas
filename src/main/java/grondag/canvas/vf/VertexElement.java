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

import java.nio.IntBuffer;
import java.util.Arrays;

public class VertexElement implements VfElement<VertexElement> {
	public final int[] data = new int[16];
	private int hashCode;
	int index;

	void compute() {
		hashCode = Arrays.hashCode(data);
	}

	@Override
	public VertexElement copy() {
		VertexElement result = new VertexElement();
		System.arraycopy(data, 0, result.data, 0, 16);
		result.hashCode = hashCode;
		return result;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void write(IntBuffer buff, final int startIndex) {
		buff.put(startIndex, data);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VertexElement) {
			return Arrays.equals(data, ((VertexElement) obj).data);
		} else {
			return false;
		}
	}
}
