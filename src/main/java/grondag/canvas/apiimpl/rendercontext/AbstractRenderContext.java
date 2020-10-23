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
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.EncodingContext;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.remove.VertexEncoderOld;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.wip.encoding.WipVertexCollectorList;
import grondag.canvas.wip.state.RenderContextState;
import grondag.canvas.wip.state.WipRenderMaterial;
import grondag.canvas.wip.state.WipRenderMaterialFinder;
import grondag.frex.api.material.MaterialMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

public abstract class AbstractRenderContext implements RenderContext {
	private static final QuadTransform NO_TRANSFORM = (q) -> true;
	private static final MaterialMap defaultMap = MaterialMap.defaultMaterialMap();
	final WipRenderMaterialFinder finder = new WipRenderMaterialFinder();
	public final float[] vecData = new float[3];
	public final int[] appendData = new int[MaterialVertexFormats.MAX_QUAD_INT_STRIDE];
	public final RenderContextState contextState = new RenderContextState();
	public final WipVertexCollectorList collectors = new WipVertexCollectorList(contextState);
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
	protected boolean isFluidModel = false;
	private QuadTransform activeTransform = NO_TRANSFORM;
	protected final VertexEncoderOld encoder;

	protected AbstractRenderContext(String name, VertexEncoderOld encoder) {
		this.name = name;
		this.encoder = encoder;

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
		if (isFluidModel || materialMap == defaultMap) {
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

	public abstract EncodingContext materialContext();

	public abstract VertexConsumer consumer(WipRenderMaterial mat);

	public abstract int indexedColor(int colorIndex);

	/**
	 * Used in contexts with a fixed brightness, like ITEM.
	 */
	public abstract int brightness();

	/**
	 * Null in some contexts, like ITEM.
	 */
	public abstract @Nullable
	AoCalculator aoCalc();

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

	protected abstract BlendMode defaultBlendMode();

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
			final WipRenderMaterial mat = finder.find();
			quad.material(mat);
			encoder.encodeQuad(quad, this);
		}
	}

	protected void adjustMaterial() {
		if (finder.blendMode() == BlendMode.DEFAULT) {
			finder.blendMode(defaultBlendMode()).disableAo();
		}
	}
}
