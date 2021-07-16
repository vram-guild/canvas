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

package grondag.canvas.varia;

import java.util.BitSet;

public class FixedCapacityIndexAllocator {
	private final int indexCapacity;
	private final BitSet bits;
	private int inUseCount = 0;
	private int nextIndex = 0;

	public FixedCapacityIndexAllocator(int indexCapacity) {
		this.indexCapacity = indexCapacity;
		bits = new BitSet(indexCapacity);
	}

	public synchronized int claimIndex() {
		if (inUseCount < indexCapacity) {
			++inUseCount;

			int result = bits.nextClearBit(nextIndex);

			if (result >= indexCapacity) {
				nextIndex = 0;
			}

			result = bits.nextClearBit(nextIndex);

			assert result < indexCapacity;
			assert result >= 0;

			bits.set(result);
			nextIndex = result + 1;

			return result;
		}

		assert false : "Unable to claim index - may have reached capacity";
		return 0;
	}

	public synchronized void releaseIndex(final int index) {
		assert bits.get(index);
		bits.clear(index);
		++inUseCount;
	}

	public synchronized void clear() {
		bits.clear();
		inUseCount = 0;
		nextIndex = 0;
	}
}
