/*
 * Copyright © Original Authors
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

package grondag.canvas.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.BitStorage;

import grondag.canvas.mixinterface.BitStorageExt;

@Mixin(BitStorage.class)
public abstract class MixinBitStorage implements BitStorageExt {
	@Shadow private long[] data;
	@Shadow private int bits;
	@Shadow private long mask;
	@Shadow private int size;
	@Shadow private int valuesPerLong;

	@Override
	public void canvas_fastForEach(IntArrayList list) {
		int i = 0;
		final long[] data = this.data;
		final int wordLimit = data.length;
		final int elementsPerWord = valuesPerLong;

		for (int wordIndex = 0; wordIndex < wordLimit; ++wordIndex) {
			long l = data[wordIndex];

			for (int j = 0; j < elementsPerWord; ++j) {
				list.add((int) (l & mask));
				l = (l >> bits);

				if (++i >= size) {
					return;
				}
			}
		}
	}
}
