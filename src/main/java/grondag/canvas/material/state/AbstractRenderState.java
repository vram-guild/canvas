/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.material.state;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.DecalRenderState;
import grondag.canvas.material.property.DepthTestRenderState;
import grondag.canvas.material.property.TargetRenderState;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.property.TransparencyRenderState;
import grondag.canvas.material.property.WriteMaskRenderState;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ProgramType;

abstract class AbstractRenderState extends AbstractRenderStateView {
	public final int index;

	public final TextureMaterialState texture;
	public final String textureIdString;
	public final boolean blur;
	public final TransparencyRenderState transparency;
	public final DepthTestRenderState depthTest;
	public final boolean cull;
	public final WriteMaskRenderState writeMask;
	public final boolean enableGlint;
	public final DecalRenderState decal;
	public final boolean sorted;
	public final TargetRenderState target;
	public final boolean lines;
	public final boolean fog;
	public final MaterialShaderId shaderId;

	public final int vertexShaderIndex;
	public final ResourceLocation vertexShaderId;
	public final String vertexShader;
	public final int fragmentShaderIndex;
	public final ResourceLocation fragmentShaderId;
	public final String fragmentShader;
	public final MaterialShaderImpl shader;
	public final MaterialShaderImpl guiShader;
	public final MaterialShaderImpl terrainShader;

	public final int depthVertexShaderIndex;
	public final ResourceLocation depthVertexShaderId;
	public final String depthVertexShader;
	public final int depthFragmentShaderIndex;
	public final ResourceLocation depthFragmentShaderId;
	public final String depthFragmentShader;
	public final MaterialShaderImpl depthShader;
	public final MaterialShaderImpl terrainDepthShader;
	/**
	 * Will be always visible condition in vertex-controlled render state.
	 * This is ensured by the state mask.
	 */
	public final MaterialConditionImpl condition;

	public final int preset;
	public final boolean emissive;
	public final boolean disableDiffuse;
	public final boolean disableAo;
	public final boolean disableColorIndex;
	public final int cutout;
	public final boolean unmipped;
	public final boolean hurtOverlay;
	public final boolean flashOverlay;
	public final boolean castShadows;
	public final boolean primaryTargetTransparency;
	// PERF: use this to avoid overhead of animated textures
	public final boolean discardsTexture;

	protected AbstractRenderState(int index, long bits) {
		super(bits);
		this.index = index;
		texture = textureState();
		textureIdString = texture == null ? "null" : texture.id.toString();
		blur = blur();
		depthTest = DepthTestRenderState.fromIndex(depthTest());
		cull = cull();
		writeMask = WriteMaskRenderState.fromIndex(writeMask());
		enableGlint = enableGlint();
		decal = DecalRenderState.fromIndex(decal());
		target = TargetRenderState.fromIndex(target());
		lines = lines();
		fog = fog();
		condition = condition();
		transparency = TransparencyRenderState.fromIndex(transparency());
		sorted = sorted();
		shaderId = shaderId();
		vertexShaderIndex = shaderId.vertexIndex;
		vertexShaderId = shaderId.vertexId;
		vertexShader = vertexShaderId.toString();
		fragmentShaderIndex = shaderId.fragmentIndex;
		fragmentShaderId = shaderId.fragmentId;
		fragmentShader = fragmentShaderId.toString();

		depthVertexShaderIndex = shaderId.depthVertexIndex;
		depthVertexShaderId = shaderId.depthVertexId;
		depthVertexShader = depthVertexShaderId.toString();
		depthFragmentShaderIndex = shaderId.depthFragmentIndex;
		depthFragmentShaderId = shaderId.depthFragmentId;
		depthFragmentShader = depthFragmentShaderId.toString();

		primaryTargetTransparency = primaryTargetTransparency();
		shader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		guiShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		depthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH);
		terrainShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR_TERRAIN);
		terrainDepthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH_TERRAIN);
		preset = preset();
		emissive = emissive();
		disableDiffuse = disableDiffuse();
		disableAo = disableAo();
		disableColorIndex = disableColorIndex();
		cutout = cutout();
		unmipped = unmipped();
		hurtOverlay = hurtOverlay();
		flashOverlay = flashOverlay();
		castShadows = castShadows();
		discardsTexture = discardsTexture();
	}
}
