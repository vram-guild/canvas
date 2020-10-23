/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.remove;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.material.MaterialShaderImpl;
import grondag.canvas.shader.ShaderPass;
import grondag.frex.api.material.RenderMaterial;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

/**
 * Mesh material with a specific blend mode and one or more layers.
 * This class controls overall quad buffer prep and encoding, but individual
 * layers control which buffers are targeted and individual layers.
 * <p>
 * WIP2: make sure can handle "dual" render layers and similar vanilla constructs.
 */
public class MeshMaterialOld extends AbstractMeshMaterialOld implements RenderMaterial {
	/**
	 * True if base layer is translucent.
	 */
	public final boolean isTranslucent;
	public final int shaderFlags;
	@Deprecated
	public final ShaderPass shaderType;
	final MaterialConditionImpl condition;
	private final MaterialShaderImpl shader;
	public final int index;

	protected MeshMaterialOld(int index, long bits) {
		super(bits);
		this.index = index;
		condition = super.condition();
		shader = super.shader();
		isTranslucent = (blendMode() == BlendMode.TRANSLUCENT);
		// WIP2: find way to activate decal pass again
		shaderType = isTranslucent ? ShaderPass.TRANSLUCENT : ShaderPass.SOLID;

		// WIP2: flags get conveyed via MaterialVertexState instead
		int flags = emissive() ? 1 : 0;

		if (disableDiffuse()) {
			flags |= 2;
		}

		if (disableAo()) {
			flags |= 4;
		}

		switch (blendMode()) {
			case CUTOUT:
				flags |= 16; // disable LOD
				//$FALL-THROUGH$
			case CUTOUT_MIPPED:
				flags |= 8; // cutout
				break;
			default:
				break;
		}

		shaderFlags = flags;
	}

	@Override
	public MaterialShaderImpl shader() {
		return shader;
	}

	@Override
	public MaterialConditionImpl condition() {
		return condition;
	}

	static final ObjectArrayList<MeshMaterialOld> LIST = new ObjectArrayList<>();
	static final Long2ObjectOpenHashMap<MeshMaterialOld> MAP = new Long2ObjectOpenHashMap<>();

	public static MeshMaterialOld fromIndex(int index) {
		assert index < LIST.size();
		assert index >= 0;

		return LIST.get(index);
	}
}
