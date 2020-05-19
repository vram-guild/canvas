/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/


package grondag.canvas.apiimpl.rendercontext;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.encoding.VertexEncoders;

/**
 * Consumer for vanilla baked models. Generally intended to give visual results matching a vanilla render,
 * however there could be subtle (and desirable) lighting variations so is good to be able to render
 * everything consistently.
 *
 * <p>Also, the API allows multi-part models that hold multiple vanilla models to render them without
 * combining quad lists, but the vanilla logic only handles one model per block. To route all of
 * them through vanilla logic would require additional hooks.
 *
 *  <p>Works by copying the quad data to an "editor" quad held in the instance,
 *  where all transformations are applied before buffering. Transformations should be
 *  the same as they would be in a vanilla render - the editor is serving mainly
 *  as a way to access vertex data without magical numbers. It also allows a consistent interface
 *  for downstream tesselation routines.
 *
 *  <p>Another difference from vanilla render is that all transformation happens before the
 *  vertex data is sent to the byte buffer.  Generally POJO array access will be faster than
 *  manipulating the data via NIO.
 */
public class FallbackConsumer implements Consumer<BakedModel> {
	protected static CompositeMaterial MATERIAL_FLAT = Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).disableAo(0, true).find();
	protected static CompositeMaterial MATERIAL_SHADED = Canvas.INSTANCE.materialFinder().disableAo(0, true).find();
	protected static CompositeMaterial MATERIAL_AO_FLAT = Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).find();
	protected static CompositeMaterial MATERIAL_AO_SHADED = Canvas.INSTANCE.materialFinder().find();

	protected final AbstractRenderContext context;

	private final int[] editorBuffer = new int[MeshEncodingHelper.MAX_QUAD_STRIDE];

	public FallbackConsumer(AbstractRenderContext context) {
		this.context = context;
	}

	private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
		{
			data = editorBuffer;
			material(MATERIAL_SHADED);
		}

		@Override
		public QuadEmitter emit() {
			// should not be called
			throw new UnsupportedOperationException("Fallback consumer does not support .emit()");
		}
	};

	@Override
	public void accept(BakedModel model) {
		final boolean useAo =  context.defaultAo() && model.useAmbientOcclusion();
		final BlockState blockState = context.blockState();

		acceptFaceQuads(Direction.DOWN, useAo, model.getQuads(blockState, Direction.DOWN, context.random()));
		acceptFaceQuads(Direction.UP, useAo, model.getQuads(blockState, Direction.UP, context.random()));
		acceptFaceQuads(Direction.NORTH, useAo, model.getQuads(blockState, Direction.NORTH, context.random()));
		acceptFaceQuads(Direction.SOUTH, useAo, model.getQuads(blockState, Direction.SOUTH, context.random()));
		acceptFaceQuads(Direction.WEST, useAo, model.getQuads(blockState, Direction.WEST, context.random()));
		acceptFaceQuads(Direction.EAST, useAo, model.getQuads(blockState, Direction.EAST, context.random()));

		acceptInsideQuads(useAo, model.getQuads(blockState, null, context.random()));
	}

	private void acceptFaceQuads(Direction face, boolean useAo, List<BakedQuad> quads) {
		final int count = quads.size();
		if (count != 0 && context.cullTest(face)) {
			if (count == 1) {
				final BakedQuad q = quads.get(0);
				renderQuad(q, face.ordinal(), q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
			} else { // > 1
				for (int j = 0; j < count; j++) {
					final BakedQuad q = quads.get(j);
					renderQuad(q, face.ordinal(), q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
				}
			}
		}
	}

	private void acceptInsideQuads(boolean useAo, List<BakedQuad> quads) {
		final int count = quads.size();
		if (count == 1) {
			final BakedQuad q = quads.get(0);
			renderQuad(q, ModelHelper.NULL_FACE_ID, q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
		} else if (count > 1) {
			for (int j = 0; j < count; j++) {
				final BakedQuad q = quads.get(j);
				renderQuad(q, ModelHelper.NULL_FACE_ID, q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
			}
		}
	}

	private void renderQuad(BakedQuad quad, int cullFaceId, CompositeMaterial defaultMaterial) {
		final MutableQuadViewImpl editorQuad = this.editorQuad;
		final int[] editorBuffer = this.editorBuffer;

		editorQuad.setupVanillaFace(cullFaceId, quad.getFace().ordinal());

		editorBuffer[MeshEncodingHelper.HEADER_COLOR_INDEX] = quad.getColorIndex();
		editorBuffer[MeshEncodingHelper.HEADER_MATERIAL] = defaultMaterial.index();
		editorBuffer[MeshEncodingHelper.HEADER_TAG] = 0;

		System.arraycopy(quad.getVertexData(), 0, editorBuffer, MeshEncodingHelper.HEADER_STRIDE, MeshEncodingHelper.BASE_QUAD_STRIDE);

		if (!context.transform(editorQuad)) {
			return;
		}

		// Can't rely on lazy computation in tesselate because needs to happen before offsets are applied
		editorQuad.geometryFlags();

		final CompositeMaterial mat = editorQuad.material().forBlendMode(context.defaultBlendModeIndex());
		editorQuad.material(mat);
		VertexEncoders.get(context.materialContext(), mat).encodeQuad(editorQuad, context);
	}
}
