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

package grondag.canvas.buffer.util;

import net.minecraft.util.Mth;

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
		return Math.max(0, Mth.log2(size) - BIN_INDEX_SHIFT);
	}

	public static final BinIndex fromIndex(int index) {
		return BINS[index];
	}

	public static final BinIndex bin(int capacityBytes) {
		assert capacityBytes > 0;
		return BINS[binIndex(capacityBytes)];
	}
}
