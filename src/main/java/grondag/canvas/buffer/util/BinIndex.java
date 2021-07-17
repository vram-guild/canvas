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

package grondag.canvas.buffer.util;

import net.minecraft.util.math.MathHelper;

public record BinIndex(int binIndex, int capacityBytes) {
	public static final int MIN_BIN_SIZE = 0x1000;
	public static final int BIN_INDEX_SHIFT = 11;
	public static final int MAX_BIN_INDEX = 18;
	public static final int BIN_COUNT = MAX_BIN_INDEX + 1;
	public static final int MAX_BIN_SIZE = MIN_BIN_SIZE << MAX_BIN_INDEX;

	static final int test = 0x1001 >> BIN_INDEX_SHIFT;

	private static final BinIndex[] BINS = new BinIndex[BIN_COUNT];

	static {
		for (int i = 0; i < BIN_COUNT; ++i) {
			BINS[i] = new BinIndex(i, MIN_BIN_SIZE << i);
		}

		assert binIndex(1) == 0;
		assert binIndex(0x0FFF) == 0;
		assert binIndex(0x1000) == 1;
		assert binIndex(0x1FFF) == 1;
		assert binIndex(0x2000) == 2;
		assert binIndex(0x0001 << (MAX_BIN_INDEX + BIN_INDEX_SHIFT)) == MAX_BIN_INDEX;
	}

	public static final int binIndex(int size) {
		return Math.max(0, MathHelper.log2(size) - BIN_INDEX_SHIFT);
	}

	public static final BinIndex fromIndex(int index) {
		return BINS[index];
	}

	public static final BinIndex bin(int capacityBytes) {
		assert capacityBytes > 0;
		return BINS[binIndex(capacityBytes)];
	}
}
