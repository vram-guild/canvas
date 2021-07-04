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

class IntElement implements VfElement<IntElement> {
	private int c0, c1, c2, c3, hashCode;
	int index;

	void set(int c0, int c1, int c2, int c3) {
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		hashCode = VfTexture.hash4(c0, c1, c2, c3);
	}

	@Override
	public IntElement copy() {
		IntElement result = new IntElement();
		result.c0 = c0;
		result.c1 = c1;
		result.c2 = c2;
		result.c3 = c3;
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
		buff.put(startIndex, c0);
		buff.put(startIndex + 1, c1);
		buff.put(startIndex + 2, c2);
		buff.put(startIndex + 3, c3);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntElement) {
			final IntElement k = (IntElement) obj;
			return k.c0 == c0 && k.c1 == c1 && k.c2 == c2 && k.c3 == c3;
		} else {
			return false;
		}
	}
}
