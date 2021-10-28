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

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.context.BaseBakedContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;
import io.vram.frex.base.renderer.mesh.RootQuadEmitter;
import io.vram.frex.base.renderer.util.EncoderUtil;

public abstract class AbstractBakedRenderContext<C extends BaseBakedContext, E> {
	protected static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();

	protected final MaterialFinder finder = MaterialFinder.newInstance();
	protected final RootQuadEmitter emitter = new Emitter();
	public final C inputContext;
	public final E encoder;

	protected MaterialMap materialMap = defaultMap;

	protected AbstractBakedRenderContext() {
		inputContext = createInputContext();
		encoder = createEncoder();
	}

	protected abstract C createInputContext();

	protected abstract E createEncoder();

	protected void shadeQuad() {
		EncoderUtil.applyFlatLighting(emitter, inputContext.flatBrightness(emitter));
		EncoderUtil.colorizeQuad(emitter, inputContext);
	}

	protected abstract void encodeQuad();

	protected abstract void adjustMaterial();

	protected void mapMaterials(BaseQuadEmitter quad) {
		if (materialMap == defaultMap) {
			return;
		}

		final TextureAtlasSprite sprite = materialMap.needsSprite() ? quad.material().texture().spriteIndex().fromIndex(quad.spriteId()) : null;
		final RenderMaterial mapped = materialMap.getMapped(sprite);

		if (mapped != null) {
			quad.material(mapped);
		}
	}

	public final QuadEmitter emitter() {
		emitter.clear();
		return emitter;
	}

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

	/**
	 * Where we handle all pre-buffer coloring, lighting, transformation, etc.
	 * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
	 */
	private class Emitter extends RootQuadEmitter {
		{
			data = new int[MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE];
			material(RenderMaterial.defaultMaterial());
		}

		@Override
		public Emitter emit() {
			complete();
			renderQuad();
			clear();
			return this;
		}
	}
}
