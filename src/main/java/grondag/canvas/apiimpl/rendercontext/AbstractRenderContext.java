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

package grondag.canvas.apiimpl.rendercontext;

import java.util.BitSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.model.util.ColorUtil;
import io.vram.frex.base.renderer.context.BaseInputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;
import io.vram.frex.base.renderer.mesh.RootQuadEmitter;

import grondag.canvas.buffer.format.AbstractEncodingContext;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.mixinterface.SpriteExt;

// UGLY: consolidate and simplify this class hierarchy
public abstract class AbstractRenderContext<C extends BaseInputContext> {
	private static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();
	final MaterialFinder finder = MaterialFinder.newInstance();

	public final AbstractEncodingContext encodingContext = new AbstractEncodingContext() { };
	public final C inputContext;

	/** null when not in world render loop/thread or when default consumer should be honored. */
	@Nullable public VertexCollectorList collectors = null;

	protected final String name;
	protected final RootQuadEmitter emitter = new Emitter();

	protected MaterialMap materialMap = defaultMap;
	protected int defaultPreset;
	protected boolean isFluidModel = false;

	public final BitSet animationBits = new BitSet();

	protected AbstractRenderContext(String name) {
		this.name = name;
		inputContext = createInputContext();
	}

	protected abstract C createInputContext();

	void mapMaterials(BaseQuadEmitter quad) {
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

	public boolean cullTest(int faceIndex) {
		return true;
	}

	protected final boolean cullTest(BaseQuadEmitter quad) {
		return cullTest(quad.cullFaceId());
	}

	public abstract boolean defaultAo();

	//protected abstract BlockState blockState();

	//public abstract int indexedColor(int colorIndex);

	/**
	 * Used in contexts with a fixed brightness, like ITEM.
	 */
	public abstract int brightness();

	public abstract void computeAo(BaseQuadEmitter quad);

	public abstract void computeFlat(BaseQuadEmitter quad);

	protected void computeFlatSimple(BaseQuadEmitter quad) {
		final int brightness = inputContext.flatBrightness(quad);
		quad.lightmap(0, ColorUtil.maxBrightness(quad.lightmap(0), brightness));
		quad.lightmap(1, ColorUtil.maxBrightness(quad.lightmap(1), brightness));
		quad.lightmap(2, ColorUtil.maxBrightness(quad.lightmap(2), brightness));
		quad.lightmap(3, ColorUtil.maxBrightness(quad.lightmap(3), brightness));
	}

	//public abstract int flatBrightness(BaseQuadEmitter quad);

	public final void renderQuad() {
		final BaseQuadEmitter quad = emitter;

		mapMaterials(quad);

		if (cullTest(quad)) {
			finder.copyFrom(quad.material());
			adjustMaterial();
			final var mat = finder.find();
			quad.material(mat);

			if (!mat.discardsTexture() && mat.texture().isAtlas()) {
				// WIP: create and use sprite method on quad
				final int animationIndex = ((SpriteExt) mat.texture().spriteIndex().fromIndex(emitter.spriteId())).canvas_animationIndex();

				if (animationIndex >= 0) {
					animationBits.set(animationIndex);
				}
			}

			encodeQuad(quad);
		}
	}

	protected abstract void encodeQuad(BaseQuadEmitter quad);

	protected void adjustMaterial() {
		final MaterialFinder finder = this.finder;

		int bm = finder.preset();

		if (bm == MaterialConstants.PRESET_DEFAULT) {
			bm = defaultPreset;
			finder.preset(MaterialConstants.PRESET_NONE);
		}

		// fully specific renderable material
		if (bm == MaterialConstants.PRESET_NONE) return;

		switch (bm) {
			case MaterialConstants.PRESET_CUTOUT: {
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(true)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			}
			case MaterialConstants.PRESET_CUTOUT_MIPPED:
				finder
					.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_HALF)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			case MaterialConstants.PRESET_TRANSLUCENT:
				finder.transparency(MaterialConstants.TRANSPARENCY_TRANSLUCENT)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_TRANSLUCENT)
					.sorted(true);
				break;
			case MaterialConstants.PRESET_SOLID:
				finder.transparency(MaterialConstants.TRANSPARENCY_NONE)
					.cutout(MaterialConstants.CUTOUT_NONE)
					.unmipped(false)
					.target(MaterialConstants.TARGET_MAIN)
					.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
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

		// only used via RenderContext.getEmitter()
		@Override
		public Emitter emit() {
			complete();
			renderQuad();
			clear();
			return this;
		}
	}
}
