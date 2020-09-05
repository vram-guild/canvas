package grondag.canvas.apiimpl;

import java.util.function.BooleanSupplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.frex.api.material.MaterialCondition;

public class MaterialConditionImpl implements MaterialCondition {
	public static final int MAX_CONDITIONS = 64;
	private static final ObjectArrayList<MaterialConditionImpl> ALL_BY_INDEX = new ObjectArrayList<>();
	public static final MaterialConditionImpl ALWAYS = new MaterialConditionImpl(() -> true, false, false);

	public static MaterialConditionImpl fromIndex(int index) {
		return ALL_BY_INDEX.get(index);
	}

	public final BooleanSupplier supplier;
	public final boolean affectItems;
	public final boolean affectBlocks;
	public final int index;

	private int frameIndex;
	private boolean result;

	MaterialConditionImpl(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		this.supplier = supplier;
		this.affectBlocks = affectBlocks;
		this.affectItems = affectItems;
		synchronized(ALL_BY_INDEX) {
			index = ALL_BY_INDEX.size();
			ALL_BY_INDEX.add(this);
			if(index >= MAX_CONDITIONS) {
				throw new IndexOutOfBoundsException("Max render condition count exceeded.");
			}
		}
	}

	public boolean compute(int frameIndex) {
		if(frameIndex == this.frameIndex) {
			return result;
		} else {
			final boolean result = supplier.getAsBoolean();
			this.result = result;
			return result;
		}
	}
}
