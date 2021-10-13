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

package grondag.canvas.material.state.wip;

import io.vram.frex.base.renderer.material.BaseMaterialView;
import io.vram.frex.base.renderer.material.BaseRenderMaterial;

import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.state.MaterialImpl;

public class CanvasRenderMaterial extends BaseRenderMaterial {
	public final MaterialImpl state;

	public CanvasRenderMaterial(BaseMaterialView finder, int index) {
		super(index, finder);

		state = MaterialImpl.finder()
			.blur(this.blur())
			.castShadows(this.castShadows())
			.conditionIndex(this.conditionIndex())
			.cull(this.cull())
			.cutout(this.cutout())
			.decal(this.decal())
			.depthTest(this.depthTest())
			.disableAo(this.disableAo())
			.disableColorIndex(this.disableColorIndex())
			.disableDiffuse(this.disableDiffuse())
			.discardsTexture(this.discardsTexture())
			.emissive(this.emissive())
			.flashOverlay(this.flashOverlay())
			.fog(this.fog())
			.foilOverlay(this.foilOverlay())
			.hurtOverlay(this.hurtOverlay())
			.label(this.label())
			.lines(this.lines())
			.preset(this.preset())
			.shaderIndex(this.shaderIndex())
			.sorted(this.sorted())
			.target(this.target())
			.textureIndex(TextureMaterialState.fromId(this.texture().id()).index)
			.transparency(this.transparency())
			.unmipped(this.unmipped())
			.writeMask(this.writeMask())
			.find();
	}
}
