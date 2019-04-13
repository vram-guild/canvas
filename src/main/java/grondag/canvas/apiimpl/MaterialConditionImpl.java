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

package grondag.canvas.apiimpl;

import java.util.function.BooleanSupplier;

import grondag.frex.api.material.MaterialCondition;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialConditionImpl implements MaterialCondition {
    public static final int MAX_CONDITIONS = 64;
    private static final ObjectArrayList<MaterialConditionImpl> ALL_BY_INDEX = new ObjectArrayList<MaterialConditionImpl>();
    public static final MaterialConditionImpl ALWAYS = new MaterialConditionImpl(() -> true, false, false);
    
    public static MaterialConditionImpl fromIndex(int index) {
        return ALL_BY_INDEX.get(index);
    }
    
    public final BooleanSupplier supplier;
    public final boolean affectItems;
    public final boolean affectBlocks;
    public final int index;
    
    private int frameIndex;
    private boolean result;
    
    MaterialConditionImpl(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
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
