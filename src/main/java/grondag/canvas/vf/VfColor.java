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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import it.unimi.dsi.fastutil.HashCommon;

import grondag.canvas.texture.TextureData;

public class VfColor extends VfTexture {
	public static final VfColor INSTANCE = new VfColor();
	private final ConcurrentHashMap<Key, Key> MAP = new ConcurrentHashMap<>();
	private final ThreadLocal<Key> SEARCH_KEY = ThreadLocal.withInitial(Key::new);
	private VfColor() {
		super(TextureData.VF_COLOR);
	}

	private final Function<Key, Key> mapFunc = k -> {
		createImageIfNeeded();
		Key result = k.copy();
		image.add(result);
		return result;
	};

	public int index(int c0, int c1, int c2, int c3) {
		// WIP: avoid threadlocal
		final Key k = SEARCH_KEY.get();
		k.set(c0, c1, c2, c3);
		return MAP.computeIfAbsent(k, mapFunc).index;
	}

	private static class Key implements VfElement {
		private int c0, c1, c2, c3, hashCode;
		private int index;

		private void set(int c0, int c1, int c2, int c3) {
			this.c0 = c0;
			this.c1 = c1;
			this.c2 = c2;
			this.c3 = c3;
			hashCode = hash4(c0, c1, c2, c3);
		}

		private Key copy() {
			Key result = new Key();
			result.set(c0, c1, c2, c3);
			return result;
		}

		@Override
		public int length() {
			return 4;
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
		public void write(IntBuffer buff) {
			final int index = this.index;
			buff.put(index, c0);
			buff.put(index + 1, c1);
			buff.put(index + 2, c2);
			buff.put(index + 3, c3);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				final Key k = (Key) obj;
				return k.c0 == c0 && k.c1 == c1 && k.c2 == c2 && k.c3 == c3;
			} else {
				return false;
			}
		}
	}

	private static int hash4(int c0, int c1, int c2, int c3) {
		return (((HashCommon.mix(c0) * 31 + HashCommon.mix(c1)) * 31) + HashCommon.mix(c2)) * 31 + HashCommon.mix(c3);
	}
}
