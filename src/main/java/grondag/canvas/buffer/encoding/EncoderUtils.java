package grondag.canvas.buffer.encoding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.texture.SpriteInfoTexture;

abstract class EncoderUtils {
	private static final int NO_AO_SHADE = 0x7F000000;

	static void bufferQuad1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexConsumer buff = context.consumer(quad.material().forDepth(0));

		int packedNormal = 0;
		float nx = 0, ny = 0, nz = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
			nx = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
		}

		for (int i = 0; i < 4; i++) {
			transformVector.set(quad.x(i), quad.y(i), quad.z(i), 1.0F);
			transformVector.transform(matrix);
			buff.vertex(transformVector.getX(), transformVector.getY(), transformVector.getZ());

			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);

			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(quad.lightmap(i));

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
					nx = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
					ny = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
					nz = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
				}
			}

			buff.normal(nx, ny, nz);
			buff.next();
		}
	}

	static void bufferQuad2(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();
		final VertexConsumer buff1  = context.consumer(mat.forDepth(0));
		final VertexConsumer buff2  = context.consumer(mat.forDepth(1));

		final float nx0, ny0, nz0;
		final float nx1, ny1, nz1;
		final float nx2, ny2, nz2;
		final float nx3, ny3, nz3;

		if (quad.hasVertexNormals()) {
			quad.populateMissingNormals();

			int transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(0));
			nx0 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny0 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz0 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(1));
			nx1 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny1 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz1 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(2));
			nx2 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny2 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz2 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(3));
			nx3 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny3 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz3 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
		} else {
			final int packedNormal = quad.packedFaceNormal();
			final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
			nx0 = nx1 = nx2 = nx3 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny0 = ny1 = ny2 = ny3 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz0 = nz1 = nz2 = nz3 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
		}

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		final float x0 = transformVector.getX();
		final float y0 = transformVector.getY();
		final float z0 = transformVector.getZ();

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		final float x1 = transformVector.getX();
		final float y1 = transformVector.getY();
		final float z1 = transformVector.getZ();

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		final float x2 = transformVector.getX();
		final float y2 = transformVector.getY();
		final float z2 = transformVector.getZ();

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		final float x3 = transformVector.getX();
		final float y3 = transformVector.getY();
		final float z3 = transformVector.getZ();

		buff1.vertex(x0, y0, z0);
		int color = quad.spriteColor(0, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(0, 0), quad.spriteV(0, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(0));
		buff1.normal(nx0, ny0, nz0);
		buff1.next();

		buff1.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(1, 0), quad.spriteV(1, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(1));
		buff1.normal(nx1, ny1, nz1);
		buff1.next();

		buff1.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(2, 0), quad.spriteV(2, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(2));
		buff1.normal(nx2, ny2, nz2);
		buff1.next();

		buff1.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(3, 0), quad.spriteV(3, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(3));
		buff1.normal(nx3, ny3, nz3);
		buff1.next();



		buff2.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(0, 1), quad.spriteV(0, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(0));
		buff2.normal(nx0, ny0, nz0);
		buff2.next();

		buff2.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(1, 1), quad.spriteV(1, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(1));
		buff2.normal(nx1, ny1, nz1);
		buff2.next();

		buff2.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(2, 1), quad.spriteV(2, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(2));
		buff2.normal(nx2, ny2, nz2);
		buff2.next();

		buff2.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(3, 1), quad.spriteV(3, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(3));
		buff2.normal(nx3, ny3, nz3);
		buff2.next();
	}

	static void bufferQuad3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();
		final VertexConsumer buff1 = context.consumer(mat.forDepth(0));
		final VertexConsumer buff2 = context.consumer(mat.forDepth(1));
		final VertexConsumer buff3 = context.consumer(mat.forDepth(2));

		final float nx0, ny0, nz0;
		final float nx1, ny1, nz1;
		final float nx2, ny2, nz2;
		final float nx3, ny3, nz3;

		if (quad.hasVertexNormals()) {
			quad.populateMissingNormals();

			int transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(0));
			nx0 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny0 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz0 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(1));
			nx1 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny1 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz1 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(2));
			nx2 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny2 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz2 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

			transformedNormal = normalMatrix.canvas_transform(quad.packedNormal(3));
			nx3 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny3 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz3 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);

		} else {
			final int packedNormal = quad.packedFaceNormal();
			final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
			nx0 = nx1 = nx2 = nx3 = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny0 = ny1 = ny2 = ny3 = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz0 = nz1 = nz2 = nz3 = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
		}

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		final float x0 = transformVector.getX();
		final float y0 = transformVector.getY();
		final float z0 = transformVector.getZ();

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		final float x1 = transformVector.getX();
		final float y1 = transformVector.getY();
		final float z1 = transformVector.getZ();

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		final float x2 = transformVector.getX();
		final float y2 = transformVector.getY();
		final float z2 = transformVector.getZ();

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		final float x3 = transformVector.getX();
		final float y3 = transformVector.getY();
		final float z3 = transformVector.getZ();

		buff1.vertex(x0, y0, z0);
		int color = quad.spriteColor(0, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(0, 0), quad.spriteV(0, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(0));
		buff1.normal(nx0, ny0, nz0);
		buff1.next();

		buff1.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(1, 0), quad.spriteV(1, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(1));
		buff1.normal(nx1, ny1, nz1);
		buff1.next();

		buff1.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(2, 0), quad.spriteV(2, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(2));
		buff1.normal(nx2, ny2, nz2);
		buff1.next();

		buff1.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(3, 0), quad.spriteV(3, 0));
		buff1.overlay(overlay);
		buff1.light(quad.lightmap(3));
		buff1.normal(nx3, ny3, nz3);
		buff1.next();



		buff2.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(0, 1), quad.spriteV(0, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(0));
		buff2.normal(nx0, ny0, nz0);
		buff2.next();

		buff2.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(1, 1), quad.spriteV(1, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(1));
		buff2.normal(nx1, ny1, nz1);
		buff2.next();

		buff2.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(2, 1), quad.spriteV(2, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(2));
		buff2.normal(nx2, ny2, nz2);
		buff2.next();

		buff2.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(3, 1), quad.spriteV(3, 1));
		buff2.overlay(overlay);
		buff2.light(quad.lightmap(3));
		buff2.normal(nx3, ny3, nz3);
		buff2.next();


		buff3.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(0, 2), quad.spriteV(0, 2));
		buff3.overlay(overlay);
		buff3.light(quad.lightmap(0));
		buff3.normal(nx0, ny0, nz0);
		buff3.next();

		buff3.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(1, 2), quad.spriteV(1, 2));
		buff3.overlay(overlay);
		buff3.light(quad.lightmap(1));
		buff3.normal(nx1, ny1, nz1);
		buff3.next();

		buff3.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(2, 2), quad.spriteV(2, 2));
		buff3.overlay(overlay);
		buff3.light(quad.lightmap(2));
		buff3.normal(nx2, ny2, nz2);
		buff3.next();

		buff3.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(3, 2), quad.spriteV(3, 2));
		buff3.overlay(overlay);
		buff3.light(quad.lightmap(3));
		buff3.normal(nx3, ny3, nz3);
		buff3.next();
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	static void colorizeQuad(MutableQuadViewImpl quad, AbstractRenderContext context, int spriteIndex) {
		final int colorIndex = quad.colorIndex();

		if (colorIndex == -1 || quad.material().disableColorIndex(spriteIndex)) {
			quad.spriteColor(0, spriteIndex, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(0, spriteIndex)));
			quad.spriteColor(1, spriteIndex, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(1, spriteIndex)));
			quad.spriteColor(2, spriteIndex, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(2, spriteIndex)));
			quad.spriteColor(3, spriteIndex, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(3, spriteIndex)));
		} else {
			final int indexedColor = context.indexedColor(colorIndex);
			quad.spriteColor(0, spriteIndex, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(0, spriteIndex))));
			quad.spriteColor(1, spriteIndex, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(1, spriteIndex))));
			quad.spriteColor(2, spriteIndex, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(2, spriteIndex))));
			quad.spriteColor(3, spriteIndex, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(3, spriteIndex))));
		}
	}

	static void bufferQuadDirect1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl.CompositeMaterial mat = quad.material();
		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.get(MaterialContext.TERRAIN, mat0);
		final int[] appendData = context.appendData;

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

		// PERF: capture and track sprite ID at bake time - will require
		// special handling to not break when receiving pre-baked coordinates
		// Need to do for multi-layer also.
		final int spriteId = SpriteInfoTexture.lookup(quad, 0);

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

			final int ao = aoData == null ? NO_AO_SHADE : ((Math.round(aoData[i] * 254) - 127) << 24);
			appendData[k++] = transformedNormal | ao;

			appendData[k++] = spriteId;
		}

		buff0.add(appendData, k);
	}

	static void bufferQuadDirect2(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();
		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.get(MaterialContext.TERRAIN, mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

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

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		appendData[0] = Float.floatToRawIntBits(transformVector.getX());
		appendData[1] = Float.floatToRawIntBits(transformVector.getY());
		appendData[2] = Float.floatToRawIntBits(transformVector.getZ());

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		appendData[9] = Float.floatToRawIntBits(transformVector.getX());
		appendData[10] = Float.floatToRawIntBits(transformVector.getY());
		appendData[11] =Float.floatToRawIntBits( transformVector.getZ());

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		appendData[18] = Float.floatToRawIntBits(transformVector.getX());
		appendData[19] = Float.floatToRawIntBits(transformVector.getY());
		appendData[20] = Float.floatToRawIntBits(transformVector.getZ());

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		appendData[27] = Float.floatToRawIntBits(transformVector.getX());
		appendData[28] = Float.floatToRawIntBits(transformVector.getY());
		appendData[29] = Float.floatToRawIntBits(transformVector.getZ());

		int packedLight = quad.lightmap(0);
		final int l0 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(1);
		final int l1 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(2);
		final int l2 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(3);
		final int l3 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		normalAo0 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[0] * 254) - 127) << 24);
		normalAo1 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[1] * 254) - 127) << 24);
		normalAo2 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[2] * 254) - 127) << 24);
		normalAo3 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[3] * 254) - 127) << 24);

		final int spriteId0 = SpriteInfoTexture.lookup(quad, 0);

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 0));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 0));
		appendData[6] = l0 | shaderFlags0;
		appendData[7] = normalAo0;
		appendData[8] = spriteId0;

		appendData[12] = quad.spriteColor(1, 0);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[15] = l1 | shaderFlags0;
		appendData[16] = normalAo1;
		appendData[17] = spriteId0;

		appendData[21] = quad.spriteColor(2, 0);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[24] = l2 | shaderFlags0;
		appendData[25] = normalAo2;
		appendData[26] = spriteId0;

		appendData[30] = quad.spriteColor(3, 0);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[33] = l3 | shaderFlags0;
		appendData[34] = normalAo3;
		appendData[35] = spriteId0;


		buff0.add(appendData, 36);

		final DrawableMaterial mat1 = mat.forDepth(1);
		final VertexCollectorImpl buff1  = context.collectors.get(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;
		final int spriteId1 = SpriteInfoTexture.lookup(quad, 1);

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;
		appendData[7] = normalAo0;
		appendData[8] = spriteId1;

		appendData[12] = quad.spriteColor(1, 1);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[15] = l1 | shaderFlags1;
		appendData[16] = normalAo1;
		appendData[17] = spriteId1;

		appendData[21] = quad.spriteColor(2, 1);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[24] = l2 | shaderFlags1;
		appendData[25] = normalAo2;
		appendData[26] = spriteId1;

		appendData[30] = quad.spriteColor(3, 1);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[33] = l3 | shaderFlags1;
		appendData[34] = normalAo3;
		appendData[35] = spriteId1;

		buff1.add(appendData, 36);
	}

	static void bufferQuadDirect3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final CompositeMaterial mat = quad.material();


		final DrawableMaterial mat0 = mat.forDepth(0);
		final VertexCollectorImpl buff0  = context.collectors.get(MaterialContext.TERRAIN, mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;

		final int[] appendData = context.appendData;
		final float[] aoData = quad.ao;

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

		transformVector.set(quad.x(0), quad.y(0), quad.z(0), 1.0F);
		transformVector.transform(matrix);
		appendData[0] = Float.floatToRawIntBits(transformVector.getX());
		appendData[1] = Float.floatToRawIntBits(transformVector.getY());
		appendData[2] = Float.floatToRawIntBits(transformVector.getZ());

		transformVector.set(quad.x(1), quad.y(1), quad.z(1), 1.0F);
		transformVector.transform(matrix);
		appendData[9] = Float.floatToRawIntBits(transformVector.getX());
		appendData[10] = Float.floatToRawIntBits(transformVector.getY());
		appendData[11] =Float.floatToRawIntBits( transformVector.getZ());

		transformVector.set(quad.x(2), quad.y(2), quad.z(2), 1.0F);
		transformVector.transform(matrix);
		appendData[18] = Float.floatToRawIntBits(transformVector.getX());
		appendData[19] = Float.floatToRawIntBits(transformVector.getY());
		appendData[20] = Float.floatToRawIntBits(transformVector.getZ());

		transformVector.set(quad.x(3), quad.y(3), quad.z(3), 1.0F);
		transformVector.transform(matrix);
		appendData[27] = Float.floatToRawIntBits(transformVector.getX());
		appendData[28] = Float.floatToRawIntBits(transformVector.getY());
		appendData[29] = Float.floatToRawIntBits(transformVector.getZ());

		int packedLight = quad.lightmap(0);
		final int l0 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(1);
		final int l1 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(2);
		final int l2 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		packedLight = quad.lightmap(3);
		final int l3 = (packedLight & 0xFF) | (((packedLight >> 16) & 0xFF) << 8);

		normalAo0 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[0] * 254) - 127) << 24);
		normalAo1 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[1] * 254) - 127) << 24);
		normalAo2 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[2] * 254) - 127) << 24);
		normalAo3 |= aoData == null ? NO_AO_SHADE : ((Math.round(aoData[3] * 254) - 127) << 24);

		final int spriteId0 = SpriteInfoTexture.lookup(quad, 0);

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 0));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 0));
		appendData[6] = l0 | shaderFlags0;
		appendData[7] = normalAo0;
		appendData[8] = spriteId0;

		appendData[12] = quad.spriteColor(1, 0);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 0));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 0));
		appendData[15] = l1 | shaderFlags0;
		appendData[16] = normalAo1;
		appendData[17] = spriteId0;

		appendData[21] = quad.spriteColor(2, 0);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 0));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 0));
		appendData[24] = l2 | shaderFlags0;
		appendData[25] = normalAo2;
		appendData[26] = spriteId0;

		appendData[30] = quad.spriteColor(3, 0);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 0));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 0));
		appendData[33] = l3 | shaderFlags0;
		appendData[34] = normalAo3;
		appendData[35] = spriteId0;

		buff0.add(appendData, 36);

		final DrawableMaterial mat1 = mat.forDepth(1);
		final VertexCollectorImpl buff1  = context.collectors.get(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;
		final int spriteId1 = SpriteInfoTexture.lookup(quad, 1);

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 1));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 1));
		appendData[6] = l0 | shaderFlags1;
		appendData[7] = normalAo0;
		appendData[8] = spriteId1;

		appendData[12] = quad.spriteColor(1, 1);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 1));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 1));
		appendData[15] = l1 | shaderFlags1;
		appendData[16] = normalAo1;
		appendData[17] = spriteId1;

		appendData[21] = quad.spriteColor(2, 1);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 1));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 1));
		appendData[24] = l2 | shaderFlags1;
		appendData[25] = normalAo2;
		appendData[26] = spriteId1;

		appendData[30] = quad.spriteColor(3, 1);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 1));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 1));
		appendData[33] = l3 | shaderFlags1;
		appendData[34] = normalAo3;
		appendData[35] = spriteId1;

		buff1.add(appendData, 36);

		final DrawableMaterial mat2 = mat.forDepth(2);
		final VertexCollectorImpl buff2  = context.collectors.get(MaterialContext.TERRAIN, mat2);
		final int shaderFlags2 = mat2.shaderFlags << 16;
		final int spriteId2 = SpriteInfoTexture.lookup(quad, 2);

		appendData[3] = quad.spriteColor(0, 2);
		appendData[4] = Float.floatToRawIntBits(quad.spriteU(0, 2));
		appendData[5] = Float.floatToRawIntBits(quad.spriteV(0, 2));
		appendData[6] = l0 | shaderFlags2;
		appendData[7] = normalAo0;
		appendData[8] = spriteId2;

		appendData[12] = quad.spriteColor(1, 2);
		appendData[13] = Float.floatToRawIntBits(quad.spriteU(1, 2));
		appendData[14] = Float.floatToRawIntBits(quad.spriteV(1, 2));
		appendData[15] = l1 | shaderFlags2;
		appendData[16] = normalAo1;
		appendData[17] = spriteId2;

		appendData[21] = quad.spriteColor(2, 2);
		appendData[22] = Float.floatToRawIntBits(quad.spriteU(2, 2));
		appendData[23] = Float.floatToRawIntBits(quad.spriteV(2, 2));
		appendData[24] = l2 | shaderFlags2;
		appendData[25] = normalAo2;
		appendData[26] = spriteId2;

		appendData[30] = quad.spriteColor(3, 2);
		appendData[31] = Float.floatToRawIntBits(quad.spriteU(3, 2));
		appendData[32] = Float.floatToRawIntBits(quad.spriteV(3, 2));
		appendData[33] = l3 | shaderFlags2;
		appendData[34] = normalAo3;
		appendData[35] = spriteId2;

		buff2.add(appendData, 36);
	}

	static void applyBlockLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		// FIX: per-vertex light maps will be ignored unless we bake a custom HD map
		// or retain vertex light maps in buffer format and logic in shader to take max

		if (!quad.material().disableAo(0) && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.aoCalc().compute(quad);
		} else {
			if (Configurator.semiFlatLighting) {
				context.aoCalc().computeFlat(quad);
			} else {
				// TODO: in HD path don't do this
				final int brightness = context.flatBrightness(quad);
				quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), brightness));
				quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), brightness));
				quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), brightness));
				quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), brightness));

				if (Configurator.hdLightmaps()) {
					context.aoCalc().computeFlatHd(quad, brightness);
				}
			}
		}
	}

	static void applyItemLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int lightmap = quad.material().emissive(0) ? VertexEncoder.FULL_BRIGHTNESS : context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
