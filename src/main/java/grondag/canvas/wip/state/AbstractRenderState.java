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

	// WIP: remove - doesn't change
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
		super(bits);
		this.index = index;
		primitive = primitive();
		texture = texture();
		bilinear = bilinear();
		depthTest = depthTest();
		cull = cull();
		writeMask = writeMask();
		enableLightmap = enableLightmap();
		decal = decal();
		target = target();
		lines = lines();
		fog = fog();
		vertexStrideInts = MaterialVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.vertexStrideInts;
		translucency = TRANSPARENCY.getValue(bits);
		shader = WipMaterialShaderManager.INSTANCE.get(SHADER.getValue(bits));
		isTranslucentTerrain = (target == WipTarget.MAIN || target == WipTarget.TRANSLUCENT) && translucency == WipTransparency.TRANSLUCENT;
	}
}
