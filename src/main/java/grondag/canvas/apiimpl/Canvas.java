/*
 * Copyright © Original Authors
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

package grondag.canvas.apiimpl;

import io.vram.frex.api.renderer.ConditionManager;
import io.vram.frex.api.renderer.MaterialShaderManager;
import io.vram.frex.api.renderer.MaterialTextureManager;
import io.vram.frex.base.renderer.BaseRenderer;
import io.vram.frex.base.renderer.material.BaseMaterialManager;
import io.vram.frex.base.renderer.material.BaseMaterialManager.MaterialFactory;

import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.material.state.RenderState;

public class Canvas extends BaseRenderer<CanvasRenderMaterial> {
	public Canvas() {
		super(CanvasRenderMaterial::new);
	}

	@Override
	protected BaseMaterialManager<CanvasRenderMaterial> createMaterialManager(ConditionManager conditions, MaterialTextureManager textures, MaterialShaderManager shaders, MaterialFactory<CanvasRenderMaterial> factory) {
		// Need to provide references to manager instances before any materials are instantiated
		RenderState.init(textures);
		return new BaseMaterialManager<>(conditionManager, textureManager, shaderManager, factory);
	}
}
