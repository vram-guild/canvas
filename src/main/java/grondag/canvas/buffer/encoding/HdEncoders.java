package grondag.canvas.buffer.encoding;

import static grondag.canvas.buffer.encoding.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.material.MeshMaterial;
import grondag.canvas.apiimpl.material.MeshMaterialLayer;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public abstract class HdEncoders {
	private static final int QUAD_STRIDE = MaterialVertexFormats.HD_TERRAIN.vertexStrideInts * 4;

	public static final VertexEncoder HD_TERRAIN_1 = new HdTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			bufferQuadHd1(quad, context);
		}
	};

	public static final VertexEncoder HD_TERRAIN_2 = new HdTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			bufferQuadHd2(quad, context);
		}
	};

	public static final VertexEncoder HD_TERRAIN_3 = new HdTerrainEncoder() {
		@Override
		public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
			// needs to happen before offsets are applied
			applyBlockLighting(quad, context);
			colorizeQuad(quad, context, 0);
			colorizeQuad(quad, context, 1);
			colorizeQuad(quad, context, 2);
			bufferQuadHd3(quad, context);
		}
	};

	static void bufferQuadHd1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final MeshMaterial mat = quad.material().get();
		final MeshMaterialLayer mat0 = mat.getLayer(0);
		final VertexCollectorImpl buff0  = context.collectors.get(mat0);
		final int[] appendData = context.appendData;

		final LightmapHd hdLight = quad.hdLight;

		assert mat.blendMode() != BlendMode.DEFAULT;

		final int shaderFlags = mat0.shaderFlags << 16;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		int k = 0;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, appendData, k);
			k += 3;

			appendData[k++] = quad.spriteColor(i, 0);

			appendData[k++] = Float.floatToRawIntBits(quad.spriteU(i, 0));
			appendData[k++] = Float.floatToRawIntBits(quad.spriteV(i, 0));

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			appendData[k++] = blockLight | (skyLight << 8) | shaderFlags;
			appendData[k++] = hdLight == null ? -1 : hdLight.coord(quad, i);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			final int ao = aoData == null ? 0xFF000000 : ((Math.round(aoData[i] * 254) - 127) << 24);
			appendData[k++] = transformedNormal | ao;
		}

		assert k == QUAD_STRIDE;

		buff0.add(appendData, k);
	}

	static void bufferQuadHd2(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();
		final MeshMaterialLayer mat0 = mat.getLayer(0);
		final VertexCollectorImpl buff0  = context.collectors.get(mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

		final LightmapHd hdLight = quad.hdLight;

		assert mat.blendMode() != BlendMode.DEFAULT;

		int normalAo0, normalAo1, normalAo2, normalAo3;

		if (quad.hasVertexNormals()) {
			quad.populateMissingNormals();
			normalAo0 = normalMatrix.canvas_transform(quad.packedNormal(0));
			normalAo1 = normalMatrix.canvas_transform(quad.packedNormal(1));
			normalAo2 = normalMatrix.canvas_transform(quad.packedNormal(2));
			normalAo3 = normalMatrix.canvas_transform(quad.packedNormal(3));

		} else {
			normalAo0 = normalAo1 = normalAo2 = normalAo3 = normalMatrix.canvas_transform(quad.packedFaceNormal());
		}

		// PERF: populate array directly - both here and in vanilla encoder
		normalAo0 |= aoData == null ? 0x7F000000 : ((Math.round(aoData[0] * 254) - 127) << 24);
		normalAo1 |= aoData == null ? 0x7F000000 : ((Math.round(aoData[1] * 254) - 127) << 24);
		normalAo2 |= aoData == null ? 0x7F000000 : ((Math.round(aoData[2] * 254) - 127) << 24);
		normalAo3 |= aoData == null ? 0x7F000000 : ((Math.round(aoData[3] * 254) - 127) << 24);

		quad.transformAndAppend(0, matrix, appendData, 0);
		appendData[7] = hdLight.coord(quad, 0);
		appendData[8] = normalAo0;

		quad.transformAndAppend(1, matrix, appendData, 9);
		appendData[16] = hdLight.coord(quad, 1);
		appendData[17] = normalAo1;

		quad.transformAndAppend(2, matrix, appendData, 18);
		appendData[25] = hdLight.coord(quad, 2);
		appendData[26] = normalAo2;

		quad.transformAndAppend(3, matrix, appendData, 27);
		appendData[34] = hdLight.coord(quad, 3);
		appendData[35] = normalAo3;

		int packedLight = quad.lightmap(0);
		final int l0 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(1);
		final int l1 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(2);
		final int l2 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(3);
		final int l3 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 0));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 0));
		appendData[6] = l0 | shaderFlags0;

		appendData[12] = quad.spriteColor(1, 0);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[15] = l1 | shaderFlags0;

		appendData[21] = quad.spriteColor(2, 0);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[24] = l2 | shaderFlags0;

		appendData[30] = quad.spriteColor(3, 0);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[33] = l3 | shaderFlags0;

		buff0.add(appendData, QUAD_STRIDE);

		final MeshMaterialLayer mat1 = mat.getLayer(1);
		final VertexCollectorImpl buff1  = context.collectors.get(mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;

		appendData[12] = quad.spriteColor(1, 1);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[15] = l1 | shaderFlags1;

		appendData[21] = quad.spriteColor(2, 1);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[24] = l2 | shaderFlags1;

		appendData[30] = quad.spriteColor(3, 1);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[33] = l3 | shaderFlags1;

		buff1.add(appendData, QUAD_STRIDE);
	}

	static void bufferQuadHd3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();


		final MeshMaterialLayer mat0 = mat.getLayer(0);
		final VertexCollectorImpl buff0  = context.collectors.get(mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

		final LightmapHd hdLight = quad.hdLight;

		assert mat.blendMode() != BlendMode.DEFAULT;


		int normalAo0, normalAo1, normalAo2, normalAo3;

		if (quad.hasVertexNormals()) {
			quad.populateMissingNormals();
			normalAo0 = normalMatrix.canvas_transform(quad.packedNormal(0));
			normalAo1 = normalMatrix.canvas_transform(quad.packedNormal(1));
			normalAo2 = normalMatrix.canvas_transform(quad.packedNormal(2));
			normalAo3 = normalMatrix.canvas_transform(quad.packedNormal(3));
		} else {
			normalAo0 = normalAo1 = normalAo2 = normalAo3 = normalMatrix.canvas_transform(quad.packedFaceNormal());
		}

		normalAo0 |= aoData == null ? 0xFF000000 : ((Math.round(aoData[0] * 254) - 127) << 24);
		normalAo1 |= aoData == null ? 0xFF000000 : ((Math.round(aoData[1] * 254) - 127) << 24);
		normalAo2 |= aoData == null ? 0xFF000000 : ((Math.round(aoData[2] * 254) - 127) << 24);
		normalAo3 |= aoData == null ? 0xFF000000 : ((Math.round(aoData[3] * 254) - 127) << 24);

		quad.transformAndAppend(0, matrix, appendData, 0);
		appendData[7] = hdLight.coord(quad, 0);
		appendData[8] = normalAo0;

		quad.transformAndAppend(1, matrix, appendData, 9);
		appendData[16] = hdLight.coord(quad, 1);
		appendData[17] = normalAo1;

		quad.transformAndAppend(2, matrix, appendData, 18);
		appendData[25] = hdLight.coord(quad, 2);
		appendData[26] = normalAo2;

		quad.transformAndAppend(3, matrix, appendData, 27);
		appendData[34] = hdLight.coord(quad, 3);
		appendData[35] = normalAo3;

		int packedLight = quad.lightmap(0);
		final int l0 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(1);
		final int l1 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(2);
		final int l2 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(3);
		final int l3 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 0));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 0));
		appendData[6] = l0 | shaderFlags0;

		appendData[12] = quad.spriteColor(1, 0);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[15] = l1 | shaderFlags0;

		appendData[21] = quad.spriteColor(2, 0);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[24] = l2 | shaderFlags0;

		appendData[30] = quad.spriteColor(3, 0);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[33] = l3 | shaderFlags0;

		buff0.add(appendData, QUAD_STRIDE);

		final MeshMaterialLayer mat1 = mat.getLayer(1);
		final VertexCollectorImpl buff1  = context.collectors.get(mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;

		appendData[12] = quad.spriteColor(1, 1);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[15] = l1 | shaderFlags1;

		appendData[21] = quad.spriteColor(2, 1);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[24] = l2 | shaderFlags1;

		appendData[30] = quad.spriteColor(3, 1);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[33] = l3 | shaderFlags1;

		buff1.add(appendData, QUAD_STRIDE);

		final MeshMaterialLayer mat2 = mat.getLayer(2);
		final VertexCollectorImpl buff2  = context.collectors.get(mat2);
		final int shaderFlags2 = mat2.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 2);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 2));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 2));
		appendData[6] = l0 | shaderFlags2;

		appendData[12] = quad.spriteColor(1, 2);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 2));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 2));
		appendData[15] = l1 | shaderFlags2;

		appendData[21] = quad.spriteColor(2, 2);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 2));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 2));
		appendData[24] = l2 | shaderFlags2;

		appendData[30] = quad.spriteColor(3, 2);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 2));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 2));
		appendData[33] = l3 | shaderFlags2;

		buff2.add(appendData, QUAD_STRIDE);
	}

	abstract static class HdTerrainEncoder extends VertexEncoder {
		HdTerrainEncoder() {
			super(MaterialVertexFormats.HD_TERRAIN);
		}

		@Override
		public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
			// flags disable diffuse and AO in shader - mainly meant for fluids
			// TODO: toggle/remove this when do smooth fluid lighting
			collector.addi(blockLight | (skyLight << 8) | (0b00000110 << 16));
		}
	}
}
