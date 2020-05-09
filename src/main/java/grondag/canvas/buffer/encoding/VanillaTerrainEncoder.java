package grondag.canvas.buffer.encoding;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.mixinterface.Matrix3fExt;

public class VanillaTerrainEncoder extends VanillaBlockEncoder {

	@Override
	protected void bufferQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexCollectorImpl buff = (VertexCollectorImpl) context.consumer(quad);
		final int[] appendData = buff.appendData;
		final float[] aoData = quad.ao;
		final RenderMaterialImpl.Value mat = quad.material();

		assert mat.blendMode(0) != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags() << 16;
		// FIX: was this needed (was different in v0)
		//		final int shaderFlags = (context.defaultAo() ? mat.shaderFlags() : mat.shaderFlags() | RenderMaterialImpl.SHADER_FLAGS_DISABLE_AO) << 16;

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

		buff.add(appendData, k);
	}

	@Override
	public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
		// flags disable diffuse and AO in shader - mainly meant for fluids
		collector.add(blockLight | (skyLight << 8) | 0b00110000);
	}
}
