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
