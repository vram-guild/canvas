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

package grondag.canvas.apiimpl.rendercontext.base;

import io.vram.frex.base.renderer.context.BaseBakedContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.util.EncoderUtil;

public abstract class BakedRenderContext<C extends BaseBakedContext> extends BaseRenderContext<C> {
	protected void shadeQuad() {
		EncoderUtil.applyFlatLighting(emitter, inputContext.flatBrightness(emitter));
		EncoderUtil.colorizeQuad(emitter, inputContext);
	}

	protected abstract void adjustMaterial();

	@Override
	public void renderQuad() {
		final BaseQuadEmitter quad = emitter;

		mapMaterials(quad);

		if (inputContext.cullTest(quad.cullFaceId())) {
			finder.copyFrom(quad.material());
			adjustMaterial();
			quad.material(finder.find());

			// needs to happen before offsets are applied
			shadeQuad();

			// Renderer-specific
			// Responsible for block offsets in terrain rendering
			encodeQuad();
		}
	}
}
