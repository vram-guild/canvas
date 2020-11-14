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

package grondag.canvas.apiimpl.rendercontext;

import java.util.Random;
import java.util.function.Consumer;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

// UGLY: consolidate and simplify this class hierarchy
public abstract class AbstractRenderContext implements RenderContext {
	private static final QuadTransform NO_TRANSFORM = (q) -> true;
	private static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();
	final MaterialFinderImpl finder = new MaterialFinderImpl();
	public final float[] vecData = new float[3];
	public final int[] appendData = new int[CanvasVertexFormats.MATERIAL_QUAD_STRIDE];

	/** null when not in world render loop/thread or when default consumer should be honored. */
	@Nullable public VertexCollectorList collectors = null;

	protected final String name;
	protected final MeshConsumer meshConsumer = new MeshConsumer(this);
	protected final MutableQuadViewImpl makerQuad = meshConsumer.editorQuad;
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
	protected Matrix4f matrix;
	protected Matrix3fExt normalMatrix;
	protected int overlay;
	protected MaterialMap materialMap = defaultMap;
	protected BlendMode defaultBlendMode;
	protected boolean isFluidModel = false;
	private QuadTransform activeTransform = NO_TRANSFORM;

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

	protected final boolean transform(MutableQuadViewImpl q) {
		return activeTransform.transform(q);
	}

	protected boolean hasTransform() {
		return activeTransform != NO_TRANSFORM;
	}

	void mapMaterials(MutableQuadViewImpl quad) {
		if (materialMap == defaultMap) {
			return;
		}

		final Sprite sprite = materialMap.needsSprite() ? SpriteInfoTexture.BLOCKS.fromId(quad.spriteId()) : null;
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
	public final Consumer<Mesh> meshConsumer() {
		return meshConsumer;
	}

	@Override
	public final Consumer<BakedModel> fallbackConsumer() {
		return fallbackConsumer;
	}

	@Override
	public final QuadEmitter getEmitter() {
		return meshConsumer.getEmitter();
	}

	// for use by fallback consumer
	protected boolean cullTest(int faceIndex) {
		return true;
	}

	protected final boolean cullTest(MutableQuadViewImpl quad) {
		return cullTest(quad.cullFaceId());
	}

	protected abstract Random random();

	public abstract boolean defaultAo();

	protected abstract BlockState blockState();

	public abstract int indexedColor(int colorIndex);

	/**
	 * Used in contexts with a fixed brightness, like ITEM.
	 */
	public abstract int brightness();

	public abstract void computeAo(MutableQuadViewImpl quad);

	public abstract void computeFlat(MutableQuadViewImpl quad);

	protected void computeFlatSimple(MutableQuadViewImpl quad) {
		final int brightness = flatBrightness(quad);
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), brightness));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), brightness));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), brightness));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), brightness));
	}

	public abstract int flatBrightness(MutableQuadViewImpl quad);

	public final int overlay() {
		return overlay;
	}

	public final Matrix4f matrix() {
		return matrix;
	}

	public final Matrix3fExt normalMatrix() {
		return normalMatrix;
	}

	public final void renderQuad() {
		final MutableQuadViewImpl quad = makerQuad;

		mapMaterials(quad);

		if (hasTransform()) {
			if (!transform(quad)) {
				return;
			}

			quad.geometryFlags();
			quad.unmapSpritesIfNeeded();
		}

		if (cullTest(quad)) {
			finder.copyFrom(quad.material());
			adjustMaterial();
			quad.material(finder.find());
			encodeQuad(quad);
		}
	}

	protected abstract void encodeQuad(MutableQuadViewImpl quad);

	protected void adjustMaterial() {
		final MaterialFinderImpl finder = this.finder;

		BlendMode bm = finder.blendMode();

		if (bm == BlendMode.DEFAULT) {
			bm = defaultBlendMode;
			finder.blendMode(null);
		}

		// fully specific renderable material
		if (bm == null) return;

		switch (bm) {
			case CUTOUT:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(true)
				.unmipped(true)
				.transparentCutout(false)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			case CUTOUT_MIPPED:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(true)
				.unmipped(false)
				.transparentCutout(false)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			case TRANSLUCENT:
				finder.transparency(MaterialFinder.TRANSPARENCY_TRANSLUCENT)
				.cutout(false)
				.unmipped(false)
				.transparentCutout(false)
				.target(MaterialFinder.TARGET_TRANSLUCENT)
				.sorted(true);
				break;
			case SOLID:
				finder.transparency(MaterialFinder.TRANSPARENCY_NONE)
				.cutout(false)
				.unmipped(false)
				.transparentCutout(false)
				.target(MaterialFinder.TARGET_MAIN)
				.sorted(false);
				break;
			default:
				assert false : "Unhandled blend mode";
		}
	}
}
