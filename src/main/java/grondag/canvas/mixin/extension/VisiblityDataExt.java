package grondag.canvas.mixin.extension;

import java.util.BitSet;
import java.util.Set;

import net.minecraft.util.math.Direction;

public interface VisiblityDataExt {
    BitSet bitSet();

    int getEmptyCount();

    void setEmptyCount(int count);

    void addExteriorToSet(int i, Set<Direction> set);

    int getNeighborIndex(int i, Direction face);
}
