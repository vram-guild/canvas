/*******************************************************************************
 * Copyright 2019 grondag
 * 
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

package grondag.canvas.buffer.packing;

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
