package grondag.canvas.buffer.encoding;
import static grondag.canvas.buffer.encoding.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.applyItemLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuad1;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuad2;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuad3;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuadDirect1;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuadDirect2;
import static grondag.canvas.buffer.encoding.EncoderUtils.bufferQuadDirect3;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;

public class VanillaEncoders {
	public static final VertexEncoder VANILLA_BLOCK_1 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			bufferQuad1(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_BLOCK_2 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			bufferQuad2(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_BLOCK_3 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			colorizeQuad(quad, context, 2);
			bufferQuad3(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_TERRAIN_1 = new VanillaTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			bufferQuadDirect1(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_TERRAIN_2 = new VanillaTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			bufferQuadDirect2(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_TERRAIN_3 = new VanillaTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			colorizeQuad(quad, context, 2);
			bufferQuadDirect3(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM_1 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			colorizeQuad(quad, context, 0);
			applyItemLighting(quad, context);
			bufferQuad1(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM_2 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			applyItemLighting(quad, context);
			bufferQuad2(quad, context);
		}
	};

	public static final VertexEncoder VANILLA_ITEM_3 = new VertexEncoder(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS) {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			colorizeQuad(quad, context, 2);
			applyItemLighting(quad, context);

			bufferQuad3(quad, context);
		}
	};

	abstract static class VanillaTerrainEncoder extends VertexEncoder {

		VanillaTerrainEncoder() {
			super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS);
		}

		@Override
		public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
			// flags disable diffuse and AO in shader - mainly meant for fluids
			// TODO: toggle/remove this when do smooth fluid lighting
			collector.add(blockLight | (skyLight << 8) | (0b00000110 << 16));
		}
	}

}
