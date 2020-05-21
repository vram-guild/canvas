package grondag.canvas.buffer.encoding;

import static grondag.canvas.buffer.encoding.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.encoding.EncoderUtils.colorizeQuad;

import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;

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
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl.CompositeMaterial mat = quad.material();
		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.getDirect(MaterialContext.TERRAIN, mat0);
		final int[] appendData = context.appendData;

		final LightmapHd aoMap = quad.aoShade == null ? LightmapHd.findAo(quad.ao) : quad.aoShade;
		final LightmapHd blockMap = quad.blockLight == null ? LightmapHd.findFlatLight(quad.lightmap(0) & 0xFF) : quad.blockLight;
		final LightmapHd skyMap = quad.skyLight == null ? LightmapHd.findFlatLight((quad.lightmap(0) >> 16) & 0xFF) : quad.skyLight;

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
			transformVector.set(quad.x(i), quad.y(i), quad.z(i), 1.0F);
			transformVector.transform(matrix);
			appendData[k++] = Float.floatToRawIntBits(transformVector.getX());
			appendData[k++] = Float.floatToRawIntBits(transformVector.getY());
			appendData[k++] = Float.floatToRawIntBits(transformVector.getZ());

			appendData[k++] = quad.spriteColor(i, 0);

			appendData[k++] = Float.floatToRawIntBits(quad.spriteU(i, 0));
			appendData[k++] = Float.floatToRawIntBits(quad.spriteV(i, 0));

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			appendData[k++] = blockLight | (skyLight << 8) | shaderFlags;

			appendData[k++] = aoMap.coord(quad, i);
			appendData[k++] = blockMap.coord(quad, i);
			appendData[k++] = skyMap.coord(quad, i);

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
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();
		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.getDirect(MaterialContext.TERRAIN, mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

		final LightmapHd aoMap = quad.aoShade == null ? LightmapHd.findAo(quad.ao) : quad.aoShade;
		final LightmapHd blockMap = quad.blockLight == null ? LightmapHd.findFlatLight(quad.lightmap(0) & 0xFF) : quad.blockLight;
		final LightmapHd skyMap = quad.skyLight == null ? LightmapHd.findFlatLight((quad.lightmap(0) >> 16) & 0xFF) : quad.skyLight;

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

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		appendData[0] = Float.floatToRawIntBits(transformVector.getX());
		appendData[1] = Float.floatToRawIntBits(transformVector.getY());
		appendData[2] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[7] = aoMap.coord(quad, 0);
		appendData[8] = blockMap.coord(quad, 0);
		appendData[9] = skyMap.coord(quad, 0);
		appendData[10] = normalAo0;

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		appendData[11] = Float.floatToRawIntBits(transformVector.getX());
		appendData[12] = Float.floatToRawIntBits(transformVector.getY());
		appendData[13] =Float.floatToRawIntBits( transformVector.getZ());
		appendData[18] = aoMap.coord(quad, 1);
		appendData[19] = blockMap.coord(quad, 1);
		appendData[20] = skyMap.coord(quad, 1);
		appendData[21] = normalAo1;

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		appendData[22] = Float.floatToRawIntBits(transformVector.getX());
		appendData[23] = Float.floatToRawIntBits(transformVector.getY());
		appendData[24] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[29] = aoMap.coord(quad, 2);
		appendData[30] = blockMap.coord(quad, 2);
		appendData[31] = skyMap.coord(quad, 2);
		appendData[32] = normalAo2;

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		appendData[33] = Float.floatToRawIntBits(transformVector.getX());
		appendData[34] = Float.floatToRawIntBits(transformVector.getY());
		appendData[35] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[40] = aoMap.coord(quad, 3);
		appendData[41] = blockMap.coord(quad, 3);
		appendData[42] = skyMap.coord(quad, 3);
		appendData[43] = normalAo3;

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

		appendData[14] = quad.spriteColor(1, 0);
		appendData[15] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[16] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[17] = l1 | shaderFlags0;

		appendData[25] = quad.spriteColor(2, 0);
		appendData[26] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[27] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[28] = l2 | shaderFlags0;

		appendData[36] = quad.spriteColor(3, 0);
		appendData[37] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[38] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[39] = l3 | shaderFlags0;

		buff0.add(appendData, QUAD_STRIDE);

		final DrawableMaterial mat1 = mat.forDepth(1);
		final VertexCollectorImpl buff1  = context.collectors.getDirect(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;

		appendData[14] = quad.spriteColor(1, 1);
		appendData[15] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[16] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[17] = l1 | shaderFlags1;

		appendData[25] = quad.spriteColor(2, 1);
		appendData[26] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[27] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[28] = l2 | shaderFlags1;

		appendData[36] = quad.spriteColor(3, 1);
		appendData[37] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[38] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[39] = l3 | shaderFlags1;

		buff1.add(appendData, QUAD_STRIDE);
	}

	static void bufferQuadHd3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();


		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.getDirect(MaterialContext.TERRAIN, mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

		final LightmapHd aoMap = quad.aoShade == null ? LightmapHd.findAo(quad.ao) : quad.aoShade;
		final LightmapHd blockMap = quad.blockLight == null ? LightmapHd.findFlatLight(quad.lightmap(0) & 0xFF) : quad.blockLight;
		final LightmapHd skyMap = quad.skyLight == null ? LightmapHd.findFlatLight((quad.lightmap(0) >> 16) & 0xFF) : quad.skyLight;

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

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		appendData[0] = Float.floatToRawIntBits(transformVector.getX());
		appendData[1] = Float.floatToRawIntBits(transformVector.getY());
		appendData[2] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[7] = aoMap.coord(quad, 0);
		appendData[8] = blockMap.coord(quad, 0);
		appendData[9] = skyMap.coord(quad, 0);
		appendData[10] = normalAo0;

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		appendData[11] = Float.floatToRawIntBits(transformVector.getX());
		appendData[12] = Float.floatToRawIntBits(transformVector.getY());
		appendData[13] =Float.floatToRawIntBits( transformVector.getZ());
		appendData[18] = aoMap.coord(quad, 1);
		appendData[19] = blockMap.coord(quad, 1);
		appendData[20] = skyMap.coord(quad, 1);
		appendData[21] = normalAo1;

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		appendData[22] = Float.floatToRawIntBits(transformVector.getX());
		appendData[23] = Float.floatToRawIntBits(transformVector.getY());
		appendData[24] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[29] = aoMap.coord(quad, 2);
		appendData[30] = blockMap.coord(quad, 2);
		appendData[31] = skyMap.coord(quad, 2);
		appendData[32] = normalAo2;

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		appendData[33] = Float.floatToRawIntBits(transformVector.getX());
		appendData[34] = Float.floatToRawIntBits(transformVector.getY());
		appendData[35] = Float.floatToRawIntBits(transformVector.getZ());
		appendData[40] = aoMap.coord(quad, 3);
		appendData[41] = blockMap.coord(quad, 3);
		appendData[42] = skyMap.coord(quad, 3);
		appendData[43] = normalAo3;

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

		appendData[14] = quad.spriteColor(1, 0);
		appendData[15] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[16] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[17] = l1 | shaderFlags0;

		appendData[25] = quad.spriteColor(2, 0);
		appendData[26] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[27] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[28] = l2 | shaderFlags0;

		appendData[36] = quad.spriteColor(3, 0);
		appendData[37] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[38] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[39] = l3 | shaderFlags0;

		buff0.add(appendData, QUAD_STRIDE);

		final DrawableMaterial mat1 = mat.forDepth(1);
		final VertexCollectorImpl buff1  = context.collectors.getDirect(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;

		appendData[14] = quad.spriteColor(1, 1);
		appendData[15] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[16] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[17] = l1 | shaderFlags1;

		appendData[25] = quad.spriteColor(2, 1);
		appendData[26] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[27] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[28] = l2 | shaderFlags1;

		appendData[36] = quad.spriteColor(3, 1);
		appendData[37] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[38] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[39] = l3 | shaderFlags1;

		buff1.add(appendData, QUAD_STRIDE);

		final DrawableMaterial mat2 = mat.forDepth(2);
		final VertexCollectorImpl buff2  = context.collectors.getDirect(MaterialContext.TERRAIN, mat2);
		final int shaderFlags2 = mat2.shaderFlags << 16;

		appendData[3] = quad.spriteColor(0, 2);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 2));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 2));
		appendData[6] = l0 | shaderFlags2;

		appendData[14] = quad.spriteColor(1, 2);
		appendData[15] = Float.floatToRawIntBits(quad.spriteU(1, 2));
		appendData[16] = Float.floatToRawIntBits(quad.spriteV(1, 2));
		appendData[17] = l1 | shaderFlags2;

		appendData[25] = quad.spriteColor(2, 2);
		appendData[26] = Float.floatToRawIntBits(quad.spriteU(2, 2));
		appendData[27] = Float.floatToRawIntBits(quad.spriteV(2, 2));
		appendData[28] = l2 | shaderFlags2;

		appendData[36] = quad.spriteColor(3, 2);
		appendData[37] = Float.floatToRawIntBits(quad.spriteU(3, 2));
		appendData[38] = Float.floatToRawIntBits(quad.spriteV(3, 2));
		appendData[39] = l3 | shaderFlags2;

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
			collector.add(blockLight | (skyLight << 8) | (0b00000110 << 16));
		}
	}
}
