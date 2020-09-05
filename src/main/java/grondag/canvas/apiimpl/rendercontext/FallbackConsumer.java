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
import grondag.canvas.apiimpl.material.MeshMaterialLocator;
import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.FaceConstants;
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
	protected static MeshMaterialLocator MATERIAL_FLAT = Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).disableAo(0, true).find();
	protected static MeshMaterialLocator MATERIAL_SHADED = Canvas.INSTANCE.materialFinder().disableAo(0, true).find();
	protected static MeshMaterialLocator MATERIAL_AO_FLAT = Canvas.INSTANCE.materialFinder().disableDiffuse(0, true).find();
	protected static MeshMaterialLocator MATERIAL_AO_SHADED = Canvas.INSTANCE.materialFinder().find();

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
				renderQuad(q, faceIndex, q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
			} else { // > 1
				for (int j = 0; j < count; j++) {
					final BakedQuad q = quads.get(j);
					renderQuad(q, faceIndex, q.hasShade() ? (useAo ? MATERIAL_AO_SHADED : MATERIAL_SHADED) : (useAo ? MATERIAL_AO_FLAT : MATERIAL_FLAT));
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

	private void renderQuad(BakedQuad quad, int cullFaceId, MeshMaterialLocator defaultMaterial) {
		final MutableQuadViewImpl editorQuad = this.editorQuad;
		editorQuad.fromVanilla(quad, defaultMaterial, cullFaceId);
		context.mapMaterials(editorQuad);

		if (context.hasTransform()) {
			if (!context.transform(editorQuad)) {
				return;
			}

			// Can't rely on lazy computation in tesselate because needs to happen before offsets are applied
			editorQuad.geometryFlags();
			editorQuad.unmapSpritesIfNeeded();
		}

		final MeshMaterialLocator mat = editorQuad.material().withDefaultBlendMode(context.defaultBlendModeIndex());
		editorQuad.material(mat);
		VertexEncoders.get(context.materialContext(), mat).encodeQuad(editorQuad, context);
	}
}
