package grondag.canvas.buffer.encoding;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;

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

			appendData[k++] = quad.lightmap(i);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			appendData[k++] = transformedNormal;
		}

		buff.add(appendData, k);
	}
}
