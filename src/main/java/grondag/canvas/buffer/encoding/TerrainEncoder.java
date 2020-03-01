package grondag.canvas.buffer.encoding;

import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;

import net.fabricmc.fabric.impl.client.indigo.renderer.helper.NormalHelper;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;

public class TerrainEncoder extends VertexEncoder {
	TerrainEncoder() {
		super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS);
	}

	static final TerrainEncoder INSTANCE = new TerrainEncoder();

	@Override
	protected void bufferQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {
		final Matrix4f matrix = context.matrix();
		final Matrix3f normalMatrix = context.normalMatrix();
		final Vector3f normalVec = context.normalVec();
		final VertexCollectorImpl buff = (VertexCollectorImpl) context.consumer(quad);
		final int[] appendData = buff.appendData;

		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			final Vector3f faceNormal = quad.faceNormal();
			normalVec.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
			normalVec.transform(normalMatrix);
		}

		int k = 0;
		for (int i = 0; i < 4; i++) {
			// PERF: this is BS
			final Vector4f vector4f = new Vector4f(quad.x(i), quad.y(i), quad.z(i), 1.0F);
			vector4f.transform(matrix);
			appendData[k++] = Float.floatToRawIntBits(vector4f.getX());
			appendData[k++] = Float.floatToRawIntBits(vector4f.getY());
			appendData[k++] = Float.floatToRawIntBits(vector4f.getZ());

			appendData[k++] = quad.spriteColor(i, 0);

			appendData[k++] = Float.floatToRawIntBits(quad.spriteU(i, 0));
			appendData[k++] = Float.floatToRawIntBits(quad.spriteV(i, 0));

			appendData[k++] = quad.lightmap(i);

			// PERF: don't unpack and transform normal unless necessary
			if (useNormals) {
				normalVec.set(quad.normalX(i), quad.normalY(i), quad.normalZ(i));
				normalVec.transform(normalMatrix);
			}

			appendData[k++] = NormalHelper.packNormal(normalVec, 1);
		}

		buff.add(appendData, k);
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	@Override
	protected void colorizeQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {

		final int colorIndex = quad.colorIndex();

		// TODO: handle layers

		if (colorIndex == -1 || quad.material().disableColorIndex(0)) {
			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(i, 0)));
			}
		} else {
			final int indexedColor = context.indexedColor(colorIndex);

			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(i, 0))));
			}
		}
	}
}
