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

package grondag.canvas.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.PackedIntegerArray;

import grondag.canvas.chunk.PackedIntegerArrayExt;

@Mixin(PackedIntegerArray.class)
public abstract class MixinPackedIntegerArray implements PackedIntegerArrayExt {
	@Shadow private long[] storage;
	@Shadow private int elementBits;
	@Shadow private long maxValue;
	@Shadow private int size;

	@Override
	public void canvas_fastForEach(IntArrayList list) {

		final int len = storage.length;

		if (len != 0) {
			int lastWordIndex = 0;
			long currentWord = storage[0];
			long nextWord = len > 1 ? storage[1] : 0L;

			for(int i = 0; i < size; ++i) {
				final int leastBitIndex = i * elementBits;
				final int lowWordIndex = leastBitIndex >> 6;

			final int highWordIndex = (i + 1) * elementBits - 1 >> 6;
			final int lowShift = leastBitIndex ^ lowWordIndex << 6;

			if (lowWordIndex != lastWordIndex) {
				currentWord = nextWord;
				nextWord = lowWordIndex + 1 < len ? storage[lowWordIndex + 1] : 0L;
				lastWordIndex = lowWordIndex;
			}

			if (lowWordIndex == highWordIndex) {
				list.add((int)(currentWord >>> lowShift & maxValue));
			} else {
				final int highShift = 64 - lowShift;
				list.add((int)((currentWord >>> lowShift | nextWord << highShift) & maxValue));
			}
			}

		}

	}
}
