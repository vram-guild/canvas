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

package grondag.canvas.material.old;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.shader.old.OldShaderContext;
import grondag.canvas.shader.old.OldShaderProps;
import grondag.fermion.varia.Useful;

public class OldMaterialState {
	private static final int SHADER_SHIFT = Useful.bitLength(MaterialConditionImpl.MAX_CONDITIONS) + OldShaderProps.BITLENGTH;

	private static int computeIndex(MaterialShaderImpl shader, MaterialConditionImpl condition, int shaderProps) {
		return (shader.getIndex() << SHADER_SHIFT) | (shaderProps << OldShaderProps.BITLENGTH) | condition.index;
	}

	private static final Int2ObjectOpenHashMap<OldMaterialState> VALUES = new Int2ObjectOpenHashMap<>();

	public static OldMaterialState get(int index) {
		return VALUES.get(index);
	}

	public static OldMaterialState get(MaterialShaderImpl shader, MaterialConditionImpl condition, MaterialVertexFormat format, int shaderProps) {
		final int index = computeIndex(shader, condition, shaderProps);
		OldMaterialState result = VALUES.get(index);
		if(result == null) {
			synchronized(VALUES) {
				result = VALUES.get(index);
				if(result == null) {
					result = new OldMaterialState(shader, condition, format, index, shaderProps);
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
	public final long sortIndex;
	//UGLY: encapsulate
	public final int shaderProps;
	public final MaterialVertexFormat format;

	private OldMaterialState(MaterialShaderImpl shader, MaterialConditionImpl condition, MaterialVertexFormat format, int index, int shaderProps) {
		this.shader = shader;
		this.condition = condition;
		this.index = index;
		this.shaderProps = shaderProps;
		assert OldShaderProps.spriteDepth(shaderProps) > 0;
		this.format = format;
		sortIndex = (format.vertexStrideBytes << 24) | index;
	}

	public void activate(OldShaderContext context) {
		shader.activate(context, format, shaderProps);
	}

	public MaterialVertexFormat materialVertexFormat() {
		return format;
	}
}
