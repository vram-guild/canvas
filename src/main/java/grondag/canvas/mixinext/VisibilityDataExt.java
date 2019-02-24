package grondag.canvas.mixinext;

import java.util.BitSet;
import java.util.Set;

import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface VisibilityDataExt {
    BitSet bitSet();

    int getEmptyCount();

    void setEmptyCount(int count);

    void addExteriorToSet(int i, Set<Direction> set);

    int getNeighborIndex(int i, Direction face);

    /** Actually static - use to get and hold lambda */
    ObjToIntFunction<BlockPos> indexFunction();

    /** Actually static - use to get and hold ref */
    int[] exteriorIndices();
}
