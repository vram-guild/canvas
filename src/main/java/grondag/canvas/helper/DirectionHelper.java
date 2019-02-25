package grondag.canvas.helper;

import net.minecraft.util.math.Direction;

public class DirectionHelper {
    // these have to be somewhere other than the static initialize for
    // Direction/mixins thereof
    public static final Direction[] HORIZONTAL_FACES = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST };
    public static final Direction[] VERTICAL_FACES = { Direction.UP, Direction.DOWN };
}
