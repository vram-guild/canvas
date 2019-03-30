package grondag.canvas.mixinext;

import java.util.BitSet;
import java.util.Set;

import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface VisibilityDataExt {
    BitSet canvas_closed();

    int canvas_openCount();

    void canvas_openCount(int count);

    void canvas_addEdgeFaces(int i, Set<Direction> set);

    int canvas_offset(int i, Direction face);

    /** Actually static - use to get and hold lambda */
    ObjToIntFunction<BlockPos> canvas_pack();

    /** Actually static - use to get and hold ref */
    int[] canvas_edgePoints();
}
