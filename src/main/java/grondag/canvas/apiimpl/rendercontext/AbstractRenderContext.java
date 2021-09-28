/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.apiimpl.rendercontext;

import java.util.BitSet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.mesh.Mesh;
import io.vram.frex.api.mesh.QuadEditor;
import io.vram.frex.api.model.ModelRenderContext;
import io.vram.frex.api.model.QuadTransform;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.MaterialFinderImpl;

// UGLY: consolidate and simplify this class hierarchy
public abstract class AbstractRenderContext extends AbstractEncodingContext implements ModelRenderContext {
	private static final QuadTransform NO_TRANSFORM = (q) -> true;
	private static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();
	final MaterialFinderImpl finder = new MaterialFinderImpl();
	public final float[] vecData = new float[3];

	/** null when not in world render loop/thread or when default consumer should be honored. */
	@Nullable public VertexCollectorList collectors = null;

	protected final String name;
	protected final MeshConsumer meshConsumer = new MeshConsumer(this);
	protected final QuadEditorImpl makerQuad = meshConsumer.editorQuad;
	protected final FallbackConsumer fallbackConsumer = new FallbackConsumer(this);
	private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
	private final QuadTransform stackTransform = (q) -> {
		int i = transformStack.size() - 1;

		while (i >= 0) {
			if (!transformStack.get(i--).transform(q)) {
				return false;
			}
		}

		return true;
	};

	protected MaterialMap materialMap = defaultMap;
	protected int defaultBlendMode;
	protected boolean isFluidModel = false;
	private QuadTransform activeTransform = NO_TRANSFORM;

	public final BitSet animationBits = new BitSet();

	protected AbstractRenderContext(String name) {
		this.name = name;

		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: create render context " + name);
		}
	}

	public void close() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: close render context " + name);
		}
	}

	protected final boolean transform(QuadEditorImpl q) {
		return activeTransform.transform(q);
	}

	protected boolean hasTransform() {
		return activeTransform != NO_TRANSFORM;
	}

	void mapMaterials(QuadEditorImpl quad) {
		if (materialMap == defaultMap) {
			return;
		}

		final TextureAtlasSprite sprite = materialMap.needsSprite() ? quad.material().texture.atlasInfo().fromId(quad.spriteId()) : null;
		final RenderMaterial mapped = materialMap.getMapped(sprite);

		if (mapped != null) {
			quad.material(mapped);
		}
	}

	@Override
	public void pushTransform(QuadTransform transform) {
		if (transform == null) {
			throw new NullPointerException("Renderer received null QuadTransform.");
		}

		transformStack.push(transform);

		if (transformStack.size() == 1) {
			activeTransform = transform;
		} else if (transformStack.size() == 2) {
			activeTransform = stackTransform;
		}
	}

	@Override
	public void popTransform() {
		transformStack.pop();

		if (transformStack.size() == 0) {
			activeTransform = NO_TRANSFORM;
		} else if (transformStack.size() == 1) {
			activeTransform = transformStack.get(0);
		}
	}

	@Override
	public final void accept(Mesh mesh, @Nullable BlockState blockState) {
		// WIP: Implement blockstate override
		meshConsumer.accept(mesh);
	}

	@Override
	public final void accept(BakedModel model, BlockState blockState) {
		// WIP: Implement blockstate override
		fallbackConsumer.accept(model);
	}

	@Override
	public final QuadEditor quadEmitter() {
		return meshConsumer.getEmitter();
	}

	// for use by fallback consumer
	protected boolean cullTest(int faceIndex) {
		return true;
	}

	protected final boolean cullTest(QuadEditorImpl quad) {
		return cullTest(quad.cullFaceId());
	}

	public abstract boolean defaultAo();

	protected abstract BlockState blockState();

	public abstract int indexedColor(int colorIndex);

	/**
	 * Used in contexts with a fixed brightness, like ITEM.
	 */
	public abstract int brightness();

	public abstract void computeAo(QuadEditorImpl quad);

	public abstract void computeFlat(QuadEditorImpl quad);

	protected void computeFlatSimple(QuadEditorImpl quad) {
		final int brightness = flatBrightness(quad);
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), brightness));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), brightness));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), brightness));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), brightness));
	}

	public abstract int flatBrightness(QuadEditorImpl quad);

	public final void renderQuad() {
		final QuadEditorImpl quad = makerQuad;

		mapMaterials(quad);

		if (hasTransform()) {
			if (!transform(quad)) {
				return;
			}

			quad.geometryFlags();
			quad.normalizeSpritesIfNeeded();
		}

		if (cullTest(quad)) {
			finder.copyFrom(quad.material());
			adjustMaterial();
			final var mat = finder.find();
			quad.material(mat);

			if (!mat.discardsTexture && mat.texture.isAtlas()) {
				final int animationIndex = mat.texture.atlasInfo().animationIndexFromSpriteId(makerQuad.spriteId());

				if (animationIndex >= 0) {
					animationBits.set(animationIndex);
				}
			}

			encodeQuad(quad);
		}
	}

	protected abstract void encodeQuad(QuadEditorImpl quad);

	protected void adjustMaterial() {
		final MaterialFinderImpl finder = this.finder;

		int bm = finder.preset();

		if (bm == MaterialConstants.PRESET_DEFAULT) {
			bm = defaultBlendMode;
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
}
