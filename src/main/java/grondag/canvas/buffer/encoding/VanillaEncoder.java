package grondag.canvas.buffer.encoding;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.material.MaterialBufferFormat;

public class VanillaEncoder implements VertexEncoder {
	public static final VanillaEncoder INSTANCE = new VanillaEncoder();

	/**
	 * Determines color index and render layer, then routes to appropriate
	 * tesselate routine based on material properties.
	 */
	public void tesselateQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {
		// needs to happen before offsets are applied
		context.computeLighting(quad);

		colorizeQuad(quad, context);

		context.applyLighting(quad);

		bufferQuad(quad, context);
	}

	private void bufferQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {
		final Matrix4f matrix = context.matrix();
		final int overlay = context.overlay();
		final Matrix3f normalMatrix = context.normalMatrix();
		final Vector3f normalVec = context.normalVec();
		final VertexConsumer buff = context.consumer(quad);

		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			final Vector3f faceNormal = quad.faceNormal();
			normalVec.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
			normalVec.transform(normalMatrix);
		}

		for (int i = 0; i < 4; i++) {
			buff.vertex(matrix, quad.x(i), quad.y(i), quad.z(i));
			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(quad.lightmap(i));

			if (useNormals) {
				normalVec.set(quad.normalX(i), quad.normalY(i), quad.normalZ(i));
				normalVec.transform(normalMatrix);
			}

			buff.normal(normalVec.getX(), normalVec.getY(), normalVec.getZ());
			buff.next();
		}
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	private void colorizeQuad(MutableQuadViewImpl quad, VertexEncodingContext context) {

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

	@Override
	public MaterialBufferFormat outputFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int index() {
		// TODO Auto-generated method stub
		return 0;
	}
}
