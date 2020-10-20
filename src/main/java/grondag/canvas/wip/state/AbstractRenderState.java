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

package grondag.canvas.wip.state;

import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.wip.shader.WipMaterialShaderImpl;
import grondag.canvas.wip.shader.WipMaterialShaderManager;
import grondag.canvas.wip.state.property.WipDecal;
import grondag.canvas.wip.state.property.WipDepthTest;
import grondag.canvas.wip.state.property.WipFog;
import grondag.canvas.wip.state.property.WipTarget;
import grondag.canvas.wip.state.property.WipTextureState;
import grondag.canvas.wip.state.property.WipTransparency;
import grondag.canvas.wip.state.property.WipWriteMask;
import grondag.fermion.bits.BitPacker64;

@SuppressWarnings("rawtypes")
abstract class AbstractRenderState {
	protected final long bits;

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

	public final int vertexStrideInts;
	public final WipTextureState texture;
	public final boolean bilinear;
	public final WipTransparency translucency;
	public final WipDepthTest depthTest;
	public final boolean cull;
	public final WipWriteMask writeMask;
	public final boolean enableLightmap;
	public final WipDecal decal;
	public final WipTarget target;
	public final boolean lines;
	public final WipFog fog;
	public final WipMaterialShaderImpl shader;

	/**
	 * True when translucent transparency and targets the terrain layer.
	 * Should not be rendered until that framebuffer is initialized in fabulous mode
	 * or should be delayed to render with other trasnslucent when not.
	 */
	public final boolean isTranslucentTerrain;

	protected AbstractRenderState(int index, long bits) {
		this.bits = bits;
		this.index = index;
		primitive = PRIMITIVE.getValue(bits);
		texture = WipTextureState.fromIndex(TEXTURE.getValue(bits));
		bilinear = BILINEAR.getValue(bits);
		depthTest = DEPTH_TEST.getValue(bits);
		cull = CULL.getValue(bits);
		writeMask = WRITE_MASK.getValue(bits);
		enableLightmap = ENABLE_LIGHTMAP.getValue(bits);
		decal = DECAL.getValue(bits);
		target = TARGET.getValue(bits);
		lines = LINES.getValue(bits);
		fog = FOG.getValue(bits);
		vertexStrideInts = MaterialVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.vertexStrideInts;
		translucency = TRANSPARENCY.getValue(bits);
		shader = WipMaterialShaderManager.INSTANCE.find(VERTEX_SHADER.getValue(bits), FRAGMENT_SHADER.getValue(bits), translucency == WipTransparency.TRANSLUCENT ? WipProgramType.MATERIAL_VERTEX_LOGIC : WipProgramType.MATERIAL_UNIFORM_LOGIC);
		isTranslucentTerrain = (target == WipTarget.MAIN || target == WipTarget.TRANSLUCENT) && translucency == WipTransparency.TRANSLUCENT;
	}


	static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	static final BitPacker64.IntElement TEXTURE = PACKER.createIntElement(WipTextureState.MAX_TEXTURE_STATES);
	static final BitPacker64.BooleanElement BILINEAR = PACKER.createBooleanElement();

	static final BitPacker64<Void>.EnumElement<WipTransparency> TRANSPARENCY = PACKER.createEnumElement(WipTransparency.class);
	static final BitPacker64<Void>.EnumElement<WipDepthTest> DEPTH_TEST = PACKER.createEnumElement(WipDepthTest.class);
	static final BitPacker64.BooleanElement CULL = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<WipWriteMask> WRITE_MASK = PACKER.createEnumElement(WipWriteMask.class);
	static final BitPacker64.BooleanElement ENABLE_LIGHTMAP = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<WipDecal> DECAL = PACKER.createEnumElement(WipDecal.class);
	static final BitPacker64<Void>.EnumElement<WipTarget> TARGET = PACKER.createEnumElement(WipTarget.class);
	static final BitPacker64.BooleanElement LINES = PACKER.createBooleanElement();
	static final BitPacker64<Void>.EnumElement<WipFog> FOG = PACKER.createEnumElement(WipFog.class);

	// These don't affect GL state but do affect encoding - must be buffered separately
	static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);

	static final BitPacker64.IntElement VERTEX_SHADER = PACKER.createIntElement(4096);
	static final BitPacker64.IntElement FRAGMENT_SHADER = PACKER.createIntElement(4096);
}
