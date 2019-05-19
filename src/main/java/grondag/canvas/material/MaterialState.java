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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class MaterialState {
    private static final int SHADER_SHIFT = Useful.bitLength(MaterialConditionImpl.MAX_CONDITIONS) + ShaderProps.BITLENGTH;

    private static int computeIndex(MaterialShaderImpl shader, MaterialConditionImpl condition, int shaderProps) {
        return (shader.getIndex() << SHADER_SHIFT) | (shaderProps << ShaderProps.BITLENGTH) | condition.index;
    }
    
    private static final Int2ObjectOpenHashMap<MaterialState> VALUES = new Int2ObjectOpenHashMap<>();
    
    public static MaterialState get(int index) {
        return VALUES.get(index);
    }
    
    public static MaterialState get(MaterialShaderImpl shader, MaterialConditionImpl condition, int shaderProps) {
        final int index = computeIndex(shader, condition, shaderProps);
        MaterialState result = VALUES.get(index);
        if(result == null) {
            synchronized(VALUES) {
                result = VALUES.get(index);
                if(result == null) {
                    result = new MaterialState(shader, condition, index, shaderProps);
                    VALUES.put(index, result);
                }
            }
        }
        return result;
    }
    
    //UGLY: encapsulate
    public final MaterialShaderImpl shader;
    public final MaterialConditionImpl condition;
    public final int index;
    public final int sortIndex;
    //UGLY: encapsulate
    public final int shaderProps;
    
    private MaterialState(MaterialShaderImpl shader, MaterialConditionImpl condition, int index, int shaderProps) {
        this.shader = shader;
        this.condition = condition;
        this.index = index;
        this.shaderProps = shaderProps;
        assert ShaderProps.spriteDepth(shaderProps) > 0;
        this.sortIndex = (shader.piplineVertexFormat(shaderProps).vertexStrideBytes << 24) | index;
    }

    public void activate(ShaderContext context) {
       shader.activate(context, shaderProps);
    }
    
    public MaterialVertexFormat materialVertexFormat() {
        return shader.piplineVertexFormat(shaderProps);
    }
}
