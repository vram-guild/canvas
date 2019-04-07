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

package grondag.canvas.hooks;

import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.BlockPos;

public interface IMutableAxisAlignedBB {
    IMutableAxisAlignedBB set(BoundingBox box);

    IMutableAxisAlignedBB growMutable(double value);

    IMutableAxisAlignedBB growMutable(double x, double y, double z);

    BoundingBox toImmutable();

    IMutableAxisAlignedBB offsetMutable(BlockPos pos);

    BoundingBox cast();

    IMutableAxisAlignedBB expandMutable(double x, double y, double z);

}
