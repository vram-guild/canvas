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

import java.util.List;
import java.util.function.Consumer;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.wip.state.WipRenderMaterial;
import grondag.frex.api.mesh.QuadEmitter;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

/**
 * Consumer for vanilla baked models. Generally intended to give visual results matching a vanilla render,
 * however there could be subtle (and desirable) lighting variations so is good to be able to render
 * everything consistently.
 *
 * <p>Also, the API allows multi-part models that hold multiple vanilla models to render them without
 * combining quad lists, but the vanilla logic only handles one model per block. To route all of
 * them through vanilla logic would require additional hooks.
 *
 * <p>Works by copying the quad data to an "editor" quad held in the instance,
 * where all transformations are applied before buffering. Transformations should be
 * the same as they would be in a vanilla render - the editor is serving mainly
 * as a way to access vertex data without magical numbers. It also allows a consistent interface
 * for downstream tesselation routines.
 *
 * <p>Another difference from vanilla render is that all transformation happens before the
 * vertex data is sent to the byte buffer.  Generally POJO array access will be faster than
 * manipulating the data via NIO.
 */
public class FallbackConsumer implements Consumer<BakedModel> {
	protected static final int BLEND_MODE_COUNT;
	protected static final int FLAT_INDEX_START;
	protected static final int SHADED_INDEX_START;
	protected static final int AO_FLAT_INDEX_START;
	protected static final int AO_SHADED_INDEX_START;

	protected static final WipRenderMaterial[] MATERIALS;

	static {
		final BlendMode[] modes = BlendMode.values();

		BLEND_MODE_COUNT = modes.length;
		FLAT_INDEX_START = 0;
		SHADED_INDEX_START = FLAT_INDEX_START + BLEND_MODE_COUNT;
		AO_FLAT_INDEX_START = SHADED_INDEX_START + BLEND_MODE_COUNT;
		AO_SHADED_INDEX_START = AO_FLAT_INDEX_START + BLEND_MODE_COUNT;
		MATERIALS = new WipRenderMaterial[AO_SHADED_INDEX_START + BLEND_MODE_COUNT];

		for (int i = 0; i < BLEND_MODE_COUNT; ++i) {
			final BlendMode b = modes[i];

			if (b == BlendMode.DEFAULT) {
				continue;
			}

			MATERIALS[FLAT_INDEX_START + i] = Canvas.INSTANCE.materialFinder().blendMode(b).disableDiffuse(true).disableAo(true).find();
			MATERIALS[SHADED_INDEX_START + i] = Canvas.INSTANCE.materialFinder().blendMode(b).disableAo(true).find();
			MATERIALS[AO_FLAT_INDEX_START + i] = Canvas.INSTANCE.materialFinder().blendMode(b).disableDiffuse(true).find();
			MATERIALS[AO_SHADED_INDEX_START + i] = Canvas.INSTANCE.materialFinder().blendMode(b).find();
		}
	}

	protected WipRenderMaterial flatMaterial() {
		return MATERIALS[FLAT_INDEX_START + context.defaultBlendMode().ordinal()];
	}

	protected WipRenderMaterial shadedMaterial() {
		return MATERIALS[SHADED_INDEX_START + context.defaultBlendMode().ordinal()];
	}

	protected WipRenderMaterial aoFlatMaterial() {
		return MATERIALS[AO_FLAT_INDEX_START + context.defaultBlendMode().ordinal()];
	}

	protected WipRenderMaterial aoShadedMaterial() {
		return MATERIALS[AO_SHADED_INDEX_START + context.defaultBlendMode().ordinal()];
	}

	protected final AbstractRenderContext context;

	private final int[] editorBuffer = new int[MeshEncodingHelper.MAX_QUAD_STRIDE];

	private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
		{
			data = editorBuffer;
		}

		@Override
		public QuadEmitter emit() {
			// should not be called
			throw new UnsupportedOperationException("Fallback consumer does not support .emit()");
		}
	};

	public FallbackConsumer(AbstractRenderContext context) {
		this.context = context;
	}

	@Override
	public void accept(BakedModel model) {
		final boolean useAo = context.defaultAo() && model.useAmbientOcclusion();
		final BlockState blockState = context.blockState();

		acceptFaceQuads(FaceConstants.DOWN_INDEX, useAo, model.getQuads(blockState, Direction.DOWN, context.random()));
		acceptFaceQuads(FaceConstants.UP_INDEX, useAo, model.getQuads(blockState, Direction.UP, context.random()));
		acceptFaceQuads(FaceConstants.NORTH_INDEX, useAo, model.getQuads(blockState, Direction.NORTH, context.random()));
		acceptFaceQuads(FaceConstants.SOUTH_INDEX, useAo, model.getQuads(blockState, Direction.SOUTH, context.random()));
		acceptFaceQuads(FaceConstants.WEST_INDEX, useAo, model.getQuads(blockState, Direction.WEST, context.random()));
		acceptFaceQuads(FaceConstants.EAST_INDEX, useAo, model.getQuads(blockState, Direction.EAST, context.random()));

		acceptInsideQuads(useAo, model.getQuads(blockState, null, context.random()));
	}

	private void acceptFaceQuads(int faceIndex, boolean useAo, List<BakedQuad> quads) {
		final int count = quads.size();

		if (count != 0 && context.cullTest(faceIndex)) {
			if (count == 1) {
				final BakedQuad q = quads.get(0);
				renderQuad(q, faceIndex, q.hasShade() ? (useAo ? aoShadedMaterial() : shadedMaterial()) : (useAo ? aoFlatMaterial() : flatMaterial()));
			} else { // > 1
				for (int j = 0; j < count; j++) {
					final BakedQuad q = quads.get(j);
					renderQuad(q, faceIndex, q.hasShade() ? (useAo ? aoShadedMaterial() : shadedMaterial()) : (useAo ? aoFlatMaterial() : flatMaterial()));
				}
			}
		}
	}

	private void acceptInsideQuads(boolean useAo, List<BakedQuad> quads) {
		final int count = quads.size();
		if (count == 1) {
			final BakedQuad q = quads.get(0);
			renderQuad(q, ModelHelper.NULL_FACE_ID, q.hasShade() ? (useAo ? aoShadedMaterial() : shadedMaterial()) : (useAo ? aoFlatMaterial() : flatMaterial()));
		} else if (count > 1) {
			for (int j = 0; j < count; j++) {
				final BakedQuad q = quads.get(j);
				renderQuad(q, ModelHelper.NULL_FACE_ID, q.hasShade() ? (useAo ? aoShadedMaterial() : shadedMaterial()) : (useAo ? aoFlatMaterial() : flatMaterial()));
			}
		}
	}

	private void renderQuad(BakedQuad quad, int cullFaceId, WipRenderMaterial mat) {
		final MutableQuadViewImpl editorQuad = this.editorQuad;
		editorQuad.fromVanilla(quad, mat, cullFaceId);
		context.mapMaterials(editorQuad);

		if (context.hasTransform()) {
			if (!context.transform(editorQuad)) {
				return;
			}

			// Can't rely on lazy computation in tesselate because needs to happen before offsets are applied
			editorQuad.geometryFlags();
			editorQuad.unmapSpritesIfNeeded();
			mat = editorQuad.material();
		}

		if (mat.blendMode() == BlendMode.DEFAULT) {
			mat = context.finder.copyFrom(mat).blendMode(context.defaultBlendMode()).find();
			editorQuad.material(mat);
		}

		context.encoder.encodeQuad(editorQuad, context);
	}
}
