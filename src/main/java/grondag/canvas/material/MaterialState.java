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

package grondag.canvas.material;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.fermion.varia.Useful;

public class MaterialState {
    private static final int SHADER_SHIFT = Useful.bitLength(MaterialConditionImpl.MAX_CONDITIONS);
    public static final int MAX_MATERIAL_STATES = MaterialConditionImpl.MAX_CONDITIONS * MaterialShaderManager.MAX_SHADERS;

    private static int computeIndex(MaterialShaderImpl shader, MaterialConditionImpl condition) {
        return (shader.getIndex() << SHADER_SHIFT) | condition.index;
    }
    
    private static final MaterialState[] VALUES = new MaterialState[MAX_MATERIAL_STATES];
    
    public static MaterialState get(int index) {
        return VALUES[index];
    }
    
    public static MaterialState get(MaterialShaderImpl shader, MaterialConditionImpl condition) {
        final int index = computeIndex(shader, condition);
        MaterialState result = VALUES[index];
        if(result == null) {
            synchronized(VALUES) {
                result = VALUES[index];
                if(result == null) {
                    result = new MaterialState(shader, condition, index);
                    VALUES[index] = result;
                }
            }
        }
        return result;
    }
    
    public final MaterialShaderImpl shader;
    public final MaterialConditionImpl condition;
    public final int index;
    
    private MaterialState(MaterialShaderImpl shader, MaterialConditionImpl condition, int index) {
        this.shader = shader;
        this.condition = condition;
        this.index = index;
    }
}
