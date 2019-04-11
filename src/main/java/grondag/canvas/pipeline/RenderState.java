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

package grondag.canvas.pipeline;

import grondag.canvas.apiimpl.RenderConditionImpl;
import grondag.canvas.apiimpl.RenderPipelineImpl;
import grondag.fermion.varia.Useful;

public class RenderState {
    private static final int PIPELINE_SHIFT = Useful.bitLength(RenderConditionImpl.MAX_CONDITIONS);
    public static final int MAX_RENDER_STATES = RenderConditionImpl.MAX_CONDITIONS * PipelineManager.MAX_PIPELINES;

    private static int computeIndex(RenderPipelineImpl pipeline, RenderConditionImpl condition) {
        return (pipeline.getIndex() << PIPELINE_SHIFT) | condition.index;
    }
    
    private static final RenderState[] VALUES = new RenderState[MAX_RENDER_STATES];
    
    public static RenderState get(int index) {
        return VALUES[index];
    }
    
    public static RenderState get(RenderPipelineImpl pipeline, RenderConditionImpl condition) {
        final int index = computeIndex(pipeline, condition);
        RenderState result = VALUES[index];
        if(result == null) {
            synchronized(VALUES) {
                result = VALUES[index];
                if(result == null) {
                    result = new RenderState(pipeline, condition, index);
                    VALUES[index] = result;
                }
            }
        }
        return result;
    }
    
    public final RenderPipelineImpl pipeline;
    public final RenderConditionImpl condition;
    public final int index;
    
    private RenderState(RenderPipelineImpl pipeline, RenderConditionImpl condition, int index) {
        this.pipeline = pipeline;
        this.condition = condition;
        this.index = index;
    }
}
