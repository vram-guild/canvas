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
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.buffer.encoding.VertexEncodingContext;
import grondag.canvas.material.MaterialState;

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
public abstract class AbstractFallbackConsumer implements Consumer<BakedModel> {
	private static Value MATERIAL_FLAT = Canvas.INSTANCE.materialFinder().disableAo(0, true).find();
	private static Value MATERIAL_SHADED = Canvas.INSTANCE.materialFinder().find();

	private final VertexEncodingContext  encodingContext;

	private final int[] editorBuffer = new int[MeshEncodingHelper.TOTAL_QUAD_STRIDE];

	public AbstractFallbackConsumer(VertexEncodingContext encodingContext) {
		this.encodingContext = encodingContext;
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

	protected abstract Supplier<Random> randomSupplier();

	protected abstract boolean defaultAo();

	protected abstract BlockState blockState();

	@Override
	public void accept(BakedModel model) {
		final Supplier<Random> random = randomSupplier();
		final Value defaultMaterial = defaultAo() && model.useAmbientOcclusion() ? MATERIAL_SHADED : MATERIAL_FLAT;
		final BlockState blockState = blockState();

		for (int i = 0; i < 6; i++) {
			final Direction face = ModelHelper.faceFromIndex(i);
			final List<BakedQuad> quads = model.getQuads(blockState, face, random.get());
			final int count = quads.size();

			if (count != 0 && encodingContext.cullTest.test(face)) {
				for (int j = 0; j < count; j++) {
					final BakedQuad q = quads.get(j);
					renderQuad(q, i, defaultMaterial);
				}
			}
		}

		final List<BakedQuad> quads = model.getQuads(blockState, null, random.get());
		final int count = quads.size();

		if (count != 0) {
			for (int j = 0; j < count; j++) {
				final BakedQuad q = quads.get(j);
				renderQuad(q, ModelHelper.NULL_FACE_ID, defaultMaterial);
			}
		}
	}

	private void renderQuad(BakedQuad quad, int cullFaceId, Value defaultMaterial) {
		final int[] vertexData = quad.getVertexData();

		final MutableQuadViewImpl editorQuad = this.editorQuad;
		System.arraycopy(vertexData, 0, editorBuffer, MeshEncodingHelper.HEADER_STRIDE, MeshEncodingHelper.BASE_QUAD_STRIDE);
		editorQuad.cullFace(cullFaceId);
		final int lightFaceId = quad.getFace().ordinal();
		editorQuad.lightFace(lightFaceId);
		editorQuad.nominalFace(lightFaceId);
		editorQuad.colorIndex(quad.getColorIndex());
		editorQuad.material(defaultMaterial);
		editorQuad.tag(0);
		editorQuad.invalidateShape();

		if (!encodingContext.transform.transform(editorQuad)) {
			return;
		}

		if (editorQuad.material().disableAo(0)) {
			// vanilla compatibility hack
			// For flat lighting, cull face drives everything and light face is ignored.
			if (cullFaceId == ModelHelper.NULL_FACE_ID) {
				// Can't rely on lazy computation in tesselate because needs to happen before offsets are applied
				editorQuad.geometryFlags();
			} else {
				editorQuad.geometryFlags(GeometryHelper.LIGHT_FACE_FLAG | GeometryHelper.AXIS_ALIGNED_FLAG);
				editorQuad.lightFace(cullFaceId);
			}
		}

		MaterialState.get(encodingContext.materialContext(), editorQuad).encoder.encodeQuad(editorQuad, encodingContext);
	}
}
