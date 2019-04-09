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

import java.util.function.Consumer;

import grondag.canvas.pipeline.ConditionalPipeline;

@FunctionalInterface
public interface AllocationProvider {
    /**
     * If byteCount is larger than a single buffer will give consumer more than one
     * buffer w/ offsets able to contain the given byte count. Otherwise will always
     * call consumer 1X with an allocation that contains the entire byte count. If
     * more than one buffer is needed, break(s) will be at a boundary compatible
     * with all vertex formats. All vertices in the buffer(s) will share the same
     * pipeline (and thus vertex format).
     */
    void claimAllocation(ConditionalPipeline pipeline, int byteCount, Consumer<BufferDelegate> consumer);
}
