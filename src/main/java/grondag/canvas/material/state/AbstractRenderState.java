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

package grondag.canvas.material.state;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.MaterialDecal;
import grondag.canvas.material.property.MaterialDepthTest;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.property.MaterialTransparency;
import grondag.canvas.material.property.MaterialWriteMask;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ProgramType;

import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

abstract class AbstractRenderState extends AbstractRenderStateView {
	public final int index;

	/**
	 * OpenGL primitive constant. Determines number of vertices.
	 *
	 * Currently used in vanilla are...
	 * GL_LINES
	 * GL_LINE_STRIP (currently GUI only)
	 * GL_TRIANGLE_STRIP (currently GUI only)
	 * GL_TRIANGLE_FAN (currently GUI only)
	 * GL_QUADS
	 */
	public final int primitive;

	public final MaterialTextureState texture;
	public final boolean blur;
	public final MaterialTransparency transparency;
	public final MaterialDepthTest depthTest;
	public final boolean cull;
	public final MaterialWriteMask writeMask;
	public final boolean enableLightmap;
	public final MaterialDecal decal;
	public final boolean sorted;
	public final MaterialTarget target;
	public final boolean lines;
	public final MaterialFog fog;
	public final int vertexShaderIndex;
	public final Identifier vertexShaderSource;
	public final int fragmentShaderIndex;
	public final Identifier fragmentShaderSource;
	public final MaterialShaderId shaderId;
	public final MaterialShaderImpl shader;
	public final MaterialConditionImpl condition;
	public final BlendMode blendMode;
	public final boolean emissive;
	public final boolean disableDiffuse;
	public final boolean disableAo;
	public final boolean disableColorIndex;
	public final boolean cutout;
	public final boolean unmipped;
	public final boolean translucentCutout;
	public final boolean hurtOverlay;
	public final boolean flashOverlay;
	public final boolean primaryTargetTransparency;
	// PERF: use this to avoid overhead of animated textures
	public final boolean discardsTexture;
	public final ProgramType programType;

	protected AbstractRenderState(int index, long bits) {
		super(bits);
		this.index = index;
		primitive = primitive();
		texture = textureState();
		blur = blur();
		depthTest = MaterialDepthTest.fromIndex(depthTest());
		cull = cull();
		writeMask = MaterialWriteMask.fromIndex(writeMask());
		enableLightmap = enableLightmap();
		decal = MaterialDecal.fromIndex(decal());
		target = MaterialTarget.fromIndex(target());
		lines = lines();
		fog = MaterialFog.fromIndex(fog());
		condition = condition();
		transparency = MaterialTransparency.fromIndex(transparency());
		sorted = sorted();
		shaderId = shaderId();
		vertexShaderIndex = shaderId.vertexIndex;
		vertexShaderSource = shaderId.vertexId;
		fragmentShaderIndex = shaderId.fragmentIndex;
		fragmentShaderSource = shaderId.fragmentId;
		primaryTargetTransparency = primaryTargetTransparency();
		programType = primaryTargetTransparency ? ProgramType.MATERIAL_VERTEX_LOGIC : ProgramType.MATERIAL_UNIFORM_LOGIC;
		shader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, programType);
		blendMode = blendMode();
		emissive = emissive();
		disableDiffuse = disableDiffuse();
		disableAo = disableAo();
		disableColorIndex = disableColorIndex();
		cutout = cutout();
		unmipped = unmipped();
		translucentCutout = transparentCutout();
		hurtOverlay = hurtOverlay();
		flashOverlay = flashOverlay();
		discardsTexture = discardsTexture();
	}
}
