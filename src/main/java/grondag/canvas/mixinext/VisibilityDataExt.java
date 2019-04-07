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
