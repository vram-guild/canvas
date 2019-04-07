package grondag.canvas;

import java.util.function.BooleanSupplier;

import grondag.frex.api.extended.RenderCondition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RenderConditionImpl implements RenderCondition {
    public static final int MAX_CONDITIONS = 64;
    private static final ObjectArrayList<RenderConditionImpl> ALL_BY_INDEX = new ObjectArrayList<RenderConditionImpl>();
    public static final RenderConditionImpl ALWAYS = new RenderConditionImpl(() -> true, false, false);
    
    public static RenderConditionImpl fromIndex(int index) {
        return ALL_BY_INDEX.get(index);
    }
    
    public final BooleanSupplier supplier;
    public final boolean affectItems;
    public final boolean affectBlocks;
    public final int index;
    
    private int frameIndex;
    private boolean result;
    
    RenderConditionImpl(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
        this.supplier = supplier;
        this.affectBlocks = affectBlocks;
        this.affectItems = affectItems;
        synchronized(ALL_BY_INDEX) {
            this.index = ALL_BY_INDEX.size();
            ALL_BY_INDEX.add(this);
            if(this.index >= MAX_CONDITIONS) {
                throw new IndexOutOfBoundsException("Max render condition count exceeded.");
            }
        }
    }
    
    public boolean compute(int frameIndex) {
        if(frameIndex == this.frameIndex) {
            return this.result;
        } else {
            final boolean result = supplier.getAsBoolean();
            this.result = result;
            return result;
        }
    }
}
