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

import com.google.common.base.Strings;

import io.vram.frex.api.texture.MaterialTexture;

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

	public final TargetRenderState target;
	public final MaterialTexture materialTexture;
	public final TextureMaterialState texture;
	public final boolean blur;
	public final TransparencyRenderState transparency;
	public final DepthTestRenderState depthTest;
	public final boolean cull;
	public final WriteMaskRenderState writeMask;
	public final DecalRenderState decal;
	public final boolean lines;
	public final boolean sorted;
	public final boolean castShadows;

	protected final MaterialShaderImpl shader;
	protected final MaterialShaderImpl guiShader;
	protected final MaterialShaderImpl terrainShader;
	protected final MaterialShaderImpl depthShader;
	protected final MaterialShaderImpl terrainDepthShader;

	public final boolean primaryTargetTransparency;

	protected AbstractRenderState(int index, long bits) {
		super(bits);
		this.index = index;
		target = TargetRenderState.fromIndex(TARGET.getValue(bits));
		materialTexture = MaterialTexture.fromIndex(TEXTURE.getValue(bits));
		texture = TextureMaterialState.fromId(materialTexture.id());
		blur = BLUR.getValue(bits);
		transparency = TransparencyRenderState.fromIndex(TRANSPARENCY.getValue(bits));
		depthTest = DepthTestRenderState.fromIndex(DEPTH_TEST.getValue(bits));
		cull = CULL.getValue(bits);
		writeMask = WriteMaskRenderState.fromIndex(WRITE_MASK.getValue(bits));
		decal = DecalRenderState.fromIndex(DECAL.getValue(bits));
		lines = LINES.getValue(bits);
		sorted = SORTED.getValue(bits);
		castShadows = !DISABLE_SHADOWS.getValue(bits);

		final var shaderId = MaterialShaderId.get(SHADER_ID.getValue(bits));
		final var vertexShaderIndex = shaderId.vertexIndex;
		final var fragmentShaderIndex = shaderId.fragmentIndex;
		final var depthVertexShaderIndex = shaderId.depthVertexIndex;
		final var depthFragmentShaderIndex = shaderId.depthFragmentIndex;

		primaryTargetTransparency = primaryTargetTransparency();
		shader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		guiShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		depthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH);
		terrainShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR_TERRAIN);
		terrainDepthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH_TERRAIN);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("stateIndex:   ").append(index).append("\n");
		sb.append("stateKey      ").append(Strings.padStart(Long.toHexString(bits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(bits), 64, '0')).append("\n");
		sb.append("collectorKey: ").append(Strings.padStart(Long.toHexString(collectorKey()), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(collectorKey()), 64, '0')).append("\n");
		sb.append("primaryTargetTransparency: ").append(primaryTargetTransparency).append("\n");
		sb.append("target: ").append(target.name).append("\n");
		sb.append("texture: ").append(texture.index).append("  ").append(texture.id.toString()).append("\n");
		sb.append("blur: ").append(blur).append("\n");
		sb.append("transparency: ").append(transparency.name).append("\n");
		sb.append("depthTest: ").append(depthTest.name).append("\n");
		sb.append("cull: ").append(cull).append("\n");
		sb.append("writeMask: ").append(writeMask.name).append("\n");
		sb.append("decal: ").append(decal.name).append("\n");
		sb.append("lines: ").append(lines).append("\n");
		return sb.toString();
	}
}
