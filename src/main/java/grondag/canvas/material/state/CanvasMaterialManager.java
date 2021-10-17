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

import io.vram.frex.api.renderer.ConditionManager;
import io.vram.frex.api.renderer.MaterialShaderManager;
import io.vram.frex.api.renderer.MaterialTextureManager;
import io.vram.frex.base.renderer.material.BaseMaterialManager;
import io.vram.frex.base.renderer.material.BaseMaterialView;

import grondag.canvas.apiimpl.CanvasTextureManager;
import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.shader.MaterialShaderId;

public class CanvasMaterialManager extends BaseMaterialManager<CanvasRenderMaterial> {
	public static final CanvasMaterialManager INSTANCE = new CanvasMaterialManager(MaterialConditionImpl.REGISTRY, CanvasTextureManager.INSTANCE, MaterialShaderId.MANAGER);

	protected CanvasMaterialManager(ConditionManager conditions, MaterialTextureManager textures, MaterialShaderManager shaders) {
		super(conditions, textures, shaders);
	}

	@Override
	protected CanvasRenderMaterial createMaterial(BaseMaterialView finder, int index) {
		return new CanvasRenderMaterial(finder, index);
	}
}
