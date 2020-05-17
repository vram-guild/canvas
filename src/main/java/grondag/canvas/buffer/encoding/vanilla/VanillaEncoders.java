package grondag.canvas.buffer.encoding.vanilla;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.material.MaterialVertexFormats;

public class VanillaEncoders {
	public static final VertexEncoder VANILLA_BLOCK = new VanillaBlockEncoder();

	public static final VertexEncoder VANILLA_ITEM_1 = new VanillaEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			computeItemLighting(quad);

			colorizeQuad(quad, context, 0);

			applyItemLighting(quad, context);

			bufferQuad1(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM_2 = new VanillaEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			computeItemLighting(quad);

			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);

			applyItemLighting(quad, context);

			bufferQuad2(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM_3 = new VanillaEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			computeItemLighting(quad);

			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			colorizeQuad(quad, context, 2);

			applyItemLighting(quad, context);

			bufferQuad3(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_TERRAIN = new VanillaTerrainEncoder();
}
