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
import io.vram.frex.base.renderer.context.BaseInputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;
import io.vram.frex.base.renderer.mesh.RootQuadEmitter;

public abstract class BaseRenderContext<C extends BaseInputContext> {
	protected static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();

	protected final MaterialFinder finder = MaterialFinder.newInstance();
	protected final RootQuadEmitter emitter = new Emitter();
	public final C inputContext;

	protected MaterialMap materialMap = defaultMap;

	protected BaseRenderContext() {
		inputContext = createInputContext();
	}

	protected abstract C createInputContext();

	protected abstract void encodeQuad();

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

	public QuadEmitter emitter() {
		emitter.clear();
		return emitter;
	}

	public abstract void renderQuad();

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
