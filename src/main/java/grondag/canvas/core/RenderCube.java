package grondag.canvas.core;

import static grondag.fermion.world.PackedBlockPos.WORLD_BOUNDARY;
import static grondag.fermion.world.PackedBlockPos.X_MASK;
import static grondag.fermion.world.PackedBlockPos.X_SHIFT;
import static grondag.fermion.world.PackedBlockPos.Y_MASK;
import static grondag.fermion.world.PackedBlockPos.Y_SHIFT;
import static grondag.fermion.world.PackedBlockPos.Z_MASK;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class RenderCube {
    //TODO: Configurable
    private static final int CUBE_MASK = 0xFFFFFF00;
    
    /**
     * Finds the origin of the 256x256x256 render cube for the given coordinate.
     * Works for X, Y, and Z.
     */
    public static final int renderCubeOrigin(int worldCoord) {
        return worldCoord & CUBE_MASK;
    }

    /**
     * Returns coordinate value relative to its origin. Essentially a macro for
     * worldCood - {@link #renderCubeOrigin(int)}
     */
    public static final int renderCubeRelative(int worldCoord) {
        return worldCoord - renderCubeOrigin(worldCoord);
    }

    /**
     * Floating point version - retains fractional component.
     */
    public static final float renderCubeRelative(float worldCoord) {
        return worldCoord - renderCubeOrigin(MathHelper.floor(worldCoord));
    }

    /**
     * Packs cube position corresponding with the given position into a single long
     * value. For now, assume Y coordinates are limited to 0-255.
     */
    public static long getPackedOrigin(BlockPos position) {
        return pack(renderCubeOrigin(position.getX()), renderCubeOrigin(position.getY()),
                renderCubeOrigin(position.getZ()));
    }

    public static int getPackedKeyOriginX(long packedKey) {
        return (int) ((packedKey >> X_SHIFT) & X_MASK) - WORLD_BOUNDARY;
    }

    public static int getPackedKeyOriginZ(long packedKey) {
        return (int) (packedKey & Z_MASK) - WORLD_BOUNDARY;
    }

    public static int getPackedKeyOriginY(long packedKey) {
        return (int) ((packedKey >> Y_SHIFT) & Y_MASK);
    }

    private static final long pack(int x, int y, int z) {
        return ((long) (x + WORLD_BOUNDARY) & X_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT
                | ((long) (z + WORLD_BOUNDARY) & Z_MASK);
    }
}
