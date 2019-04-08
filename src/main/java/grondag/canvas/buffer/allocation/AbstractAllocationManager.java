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

package grondag.canvas.buffer.allocation;

public abstract class AbstractAllocationManager {

    /**
     * Consumer must not exceed total bytes initially requested.
     * This allows for fixed-size buffer allocation and may allow optimization of other allocation schemes.
     */
    public abstract AllocationProvider getAllocator(int totalBytes);
    
    /**
     * Override if allocation type has per-frame upkeep
     */
    protected void prepareForFrame() {}

    /**
     * Override if allocation type needs to handle;
     */
    protected void forceReload() { };
}
