/*******************************************************************************
 * Copyright 2019, 2020 grondag
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

import net.minecraft.util.collection.PackedIntegerArray;

import grondag.canvas.mixinterface.PackedIntegerArrayExt;

@Mixin(PackedIntegerArray.class)
public abstract class MixinPackedIntegerArray implements PackedIntegerArrayExt {
	@Shadow private long[] storage;
	@Shadow private int elementBits;
	@Shadow private long maxValue;
	@Shadow private int size;
	@Shadow private int field_24079;

	@Override
	public void canvas_fastForEach(IntArrayList list) {
		int i = 0;
		final long[] bits = storage;
		final int wordLimit = bits.length;
		final int elementsPerWord = field_24079;

		for (int wordIndex = 0; wordIndex < wordLimit; ++wordIndex) {
			long l = bits[wordIndex];

			for(int j = 0; j < elementsPerWord; ++j) {
				list.add((int)(l & maxValue));
				l = (l >> elementBits);

				if (++i >= size) {
					return;
				}
			}
		}
	}
}
