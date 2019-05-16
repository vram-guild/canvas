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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.chunk.PackedIntegerArrayExt;
import grondag.canvas.chunk.PackedIntegerStorageHelper;
import net.minecraft.util.PackedIntegerArray;

@Mixin(PackedIntegerArray.class)
public abstract class MixinPackedIntegerArray implements PackedIntegerArrayExt {

    @Shadow private long[] storage;
    @Shadow private int elementBits;
    @Shadow private int size;
    
    @Override
    public PackedIntegerArray canvas_copy() {
        return new PackedIntegerArray(elementBits, size, PackedIntegerStorageHelper.claimStorageCopy(storage));
    }
}
