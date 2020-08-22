package grondag.canvas.buffer.encoding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.material.MeshMaterial;
import grondag.canvas.apiimpl.material.MeshMaterialLayer;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.SpriteInfoTexture;

abstract class EncoderUtils {
	private static final int NO_AO_SHADE = 0x7F000000;

	static void bufferQuad1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexConsumer buff = context.consumer(quad.material().get().getLayer(0));

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

		final boolean emissive = quad.material().emissive(0);

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, buff);

			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);

			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(emissive ? VertexEncoder.FULL_BRIGHTNESS : quad.lightmap(i));

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
		final float[] vecData = context.vecData;
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();
		final VertexConsumer buff1  = context.consumer(mat.getLayer(0));
		final VertexConsumer buff2  = context.consumer(mat.getLayer(1));

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

		quad.transformAndAppend(0, matrix, vecData);
		final float x0 = vecData[0];
		final float y0 = vecData[1];
		final float z0 = vecData[2];

		quad.transformAndAppend(1, matrix, vecData);
		final float x1 = vecData[0];
		final float y1 = vecData[1];
		final float z1 = vecData[2];

		quad.transformAndAppend(2, matrix, vecData);
		final float x2 = vecData[0];
		final float y2 = vecData[1];
		final float z2 = vecData[2];

		quad.transformAndAppend(3, matrix, vecData);
		final float x3 = vecData[0];
		final float y3 = vecData[1];
		final float z3 = vecData[2];

		int lm0, lm1, lm2, lm3;

		if (mat.emissive(0)) {
			lm0 = VertexEncoder.FULL_BRIGHTNESS;
			lm1 = VertexEncoder.FULL_BRIGHTNESS;
			lm2 = VertexEncoder.FULL_BRIGHTNESS;
			lm3 = VertexEncoder.FULL_BRIGHTNESS;
		} else {
			lm0 = quad.lightmap(0);
			lm1 = quad.lightmap(1);
			lm2 = quad.lightmap(2);
			lm3 = quad.lightmap(3);
		}

		buff1.vertex(x0, y0, z0);
		int color = quad.spriteColor(0, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(0, 0), quad.spriteV(0, 0));
		buff1.overlay(overlay);
		buff1.light(lm0);
		buff1.normal(nx0, ny0, nz0);
		buff1.next();

		buff1.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(1, 0), quad.spriteV(1, 0));
		buff1.overlay(overlay);
		buff1.light(lm1);
		buff1.normal(nx1, ny1, nz1);
		buff1.next();

		buff1.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(2, 0), quad.spriteV(2, 0));
		buff1.overlay(overlay);
		buff1.light(lm2);
		buff1.normal(nx2, ny2, nz2);
		buff1.next();

		buff1.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(3, 0), quad.spriteV(3, 0));
		buff1.overlay(overlay);
		buff1.light(lm3);
		buff1.normal(nx3, ny3, nz3);
		buff1.next();

		if (mat.emissive(1)) {
			lm0 = VertexEncoder.FULL_BRIGHTNESS;
			lm1 = VertexEncoder.FULL_BRIGHTNESS;
			lm2 = VertexEncoder.FULL_BRIGHTNESS;
			lm3 = VertexEncoder.FULL_BRIGHTNESS;
		} else {
			lm0 = quad.lightmap(0);
			lm1 = quad.lightmap(1);
			lm2 = quad.lightmap(2);
			lm3 = quad.lightmap(3);
		}

		buff2.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(0, 1), quad.spriteV(0, 1));
		buff2.overlay(overlay);
		buff2.light(lm0);
		buff2.normal(nx0, ny0, nz0);
		buff2.next();

		buff2.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(1, 1), quad.spriteV(1, 1));
		buff2.overlay(overlay);
		buff2.light(lm1);
		buff2.normal(nx1, ny1, nz1);
		buff2.next();

		buff2.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(2, 1), quad.spriteV(2, 1));
		buff2.overlay(overlay);
		buff2.light(lm2);
		buff2.normal(nx2, ny2, nz2);
		buff2.next();

		buff2.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(3, 1), quad.spriteV(3, 1));
		buff2.overlay(overlay);
		buff2.light(lm3);
		buff2.normal(nx3, ny3, nz3);
		buff2.next();
	}

	static void bufferQuad3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final float[] vecData = context.vecData;
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();
		final VertexConsumer buff1 = context.consumer(mat.getLayer(0));
		final VertexConsumer buff2 = context.consumer(mat.getLayer(1));
		final VertexConsumer buff3 = context.consumer(mat.getLayer(2));

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

		quad.transformAndAppend(0, matrix, vecData);
		final float x0 = vecData[0];
		final float y0 = vecData[1];
		final float z0 = vecData[2];

		quad.transformAndAppend(1, matrix, vecData);
		final float x1 = vecData[0];
		final float y1 = vecData[1];
		final float z1 = vecData[2];

		quad.transformAndAppend(2, matrix, vecData);
		final float x2 = vecData[0];
		final float y2 = vecData[1];
		final float z2 = vecData[2];

		quad.transformAndAppend(3, matrix, vecData);
		final float x3 = vecData[0];
		final float y3 = vecData[1];
		final float z3 = vecData[2];

		int lm0, lm1, lm2, lm3;

		if (mat.emissive(0)) {
			lm0 = VertexEncoder.FULL_BRIGHTNESS;
			lm1 = VertexEncoder.FULL_BRIGHTNESS;
			lm2 = VertexEncoder.FULL_BRIGHTNESS;
			lm3 = VertexEncoder.FULL_BRIGHTNESS;
		} else {
			lm0 = quad.lightmap(0);
			lm1 = quad.lightmap(1);
			lm2 = quad.lightmap(2);
			lm3 = quad.lightmap(3);
		}

		buff1.vertex(x0, y0, z0);
		int color = quad.spriteColor(0, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(0, 0), quad.spriteV(0, 0));
		buff1.overlay(overlay);
		buff1.light(lm0);
		buff1.normal(nx0, ny0, nz0);
		buff1.next();

		buff1.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(1, 0), quad.spriteV(1, 0));
		buff1.overlay(overlay);
		buff1.light(lm1);
		buff1.normal(nx1, ny1, nz1);
		buff1.next();

		buff1.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(2, 0), quad.spriteV(2, 0));
		buff1.overlay(overlay);
		buff1.light(lm2);
		buff1.normal(nx2, ny2, nz2);
		buff1.next();

		buff1.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 0);
		buff1.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff1.texture(quad.spriteU(3, 0), quad.spriteV(3, 0));
		buff1.overlay(overlay);
		buff1.light(lm3);
		buff1.normal(nx3, ny3, nz3);
		buff1.next();

		if (mat.emissive(1)) {
			lm0 = VertexEncoder.FULL_BRIGHTNESS;
			lm1 = VertexEncoder.FULL_BRIGHTNESS;
			lm2 = VertexEncoder.FULL_BRIGHTNESS;
			lm3 = VertexEncoder.FULL_BRIGHTNESS;
		} else {
			lm0 = quad.lightmap(0);
			lm1 = quad.lightmap(1);
			lm2 = quad.lightmap(2);
			lm3 = quad.lightmap(3);
		}

		buff2.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(0, 1), quad.spriteV(0, 1));
		buff2.overlay(overlay);
		buff2.light(lm0);
		buff2.normal(nx0, ny0, nz0);
		buff2.next();

		buff2.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(1, 1), quad.spriteV(1, 1));
		buff2.overlay(overlay);
		buff2.light(lm1);
		buff2.normal(nx1, ny1, nz1);
		buff2.next();

		buff2.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(2, 1), quad.spriteV(2, 1));
		buff2.overlay(overlay);
		buff2.light(lm2);
		buff2.normal(nx2, ny2, nz2);
		buff2.next();

		buff2.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 1);
		buff2.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff2.texture(quad.spriteU(3, 1), quad.spriteV(3, 1));
		buff2.overlay(overlay);
		buff2.light(lm3);
		buff2.normal(nx3, ny3, nz3);
		buff2.next();

		if (mat.emissive(2)) {
			lm0 = VertexEncoder.FULL_BRIGHTNESS;
			lm1 = VertexEncoder.FULL_BRIGHTNESS;
			lm2 = VertexEncoder.FULL_BRIGHTNESS;
			lm3 = VertexEncoder.FULL_BRIGHTNESS;
		} else {
			lm0 = quad.lightmap(0);
			lm1 = quad.lightmap(1);
			lm2 = quad.lightmap(2);
			lm3 = quad.lightmap(3);
		}

		buff3.vertex(x0, y0, z0);
		color = quad.spriteColor(0, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(0, 2), quad.spriteV(0, 2));
		buff3.overlay(overlay);
		buff3.light(lm0);
		buff3.normal(nx0, ny0, nz0);
		buff3.next();

		buff3.vertex(x1, y1, z1);
		color = quad.spriteColor(1, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(1, 2), quad.spriteV(1, 2));
		buff3.overlay(overlay);
		buff3.light(lm1);
		buff3.normal(nx1, ny1, nz1);
		buff3.next();

		buff3.vertex(x2, y2, z2);
		color = quad.spriteColor(2, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(2, 2), quad.spriteV(2, 2));
		buff3.overlay(overlay);
		buff3.light(lm2);
		buff3.normal(nx2, ny2, nz2);
		buff3.next();

		buff3.vertex(x3, y3, z3);
		color = quad.spriteColor(3, 2);
		buff3.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);
		buff3.texture(quad.spriteU(3, 2), quad.spriteV(3, 2));
		buff3.overlay(overlay);
		buff3.light(lm3);
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
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final MeshMaterial mat = quad.material().get();
		final MeshMaterialLayer mat0 = mat.getLayer(0);
		final VertexCollectorImpl buff0  = context.collectors.get(MaterialContext.TERRAIN, mat0);
		final int[] appendData = context.appendData;
		final SpriteInfoTexture spriteInfo = SpriteInfoTexture.instance();

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

		final int spriteIdCoord = spriteInfo.coordinate(quad.spriteId(0));

		assert spriteIdCoord <= 0xFFFF;

		int k = 0;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, appendData, k);
			k += 3;

			appendData[k++] = quad.spriteColor(i, 0);
			appendData[k++] = quad.spriteBufferU(i, 0) | (quad.spriteBufferV(i, 0) << 16);

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

			appendData[k++] = spriteIdCoord;
		}

		buff0.add(appendData, k);
	}

	static void bufferQuadDirect2(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();
		final MeshMaterialLayer mat0 = mat.getLayer(0);
		final VertexCollectorImpl buff0  = context.collectors.get(MaterialContext.TERRAIN, mat0);
		final int shaderFlags0 = mat0.shaderFlags << 16;
		final SpriteInfoTexture spriteInfo = SpriteInfoTexture.instance();

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

		quad.transformAndAppend(0, matrix, appendData, 0);
		quad.transformAndAppend(1, matrix, appendData, 8);
		quad.transformAndAppend(2, matrix, appendData, 16);
		quad.transformAndAppend(3, matrix, appendData, 24);

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

		final int spriteIdCoord0 = spriteInfo.coordinate(quad.spriteId(0));

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = quad.spriteBufferU(0, 0) | (quad.spriteBufferV(0, 0) << 16);
		appendData[5] = l0 | shaderFlags0;
		appendData[6] = normalAo0;
		appendData[7] = spriteIdCoord0;

		appendData[11] = quad.spriteColor(1, 0);
		appendData[12] = quad.spriteBufferU(1, 0) | (quad.spriteBufferV(1, 0) << 16);
		appendData[13] = l1 | shaderFlags0;
		appendData[14] = normalAo1;
		appendData[15] = spriteIdCoord0;

		appendData[19] = quad.spriteColor(2, 0);
		appendData[20] = quad.spriteBufferU(2, 0) | (quad.spriteBufferV(2, 0) << 16);
		appendData[21] = l2 | shaderFlags0;
		appendData[22] = normalAo2;
		appendData[23] = spriteIdCoord0;

		appendData[27] = quad.spriteColor(3, 0);
		appendData[28] = quad.spriteBufferU(3, 0) | (quad.spriteBufferV(3, 0) << 16);
		appendData[29] = l3 | shaderFlags0;
		appendData[30] = normalAo3;
		appendData[31] = spriteIdCoord0;

		buff0.add(appendData, 32);

		final MeshMaterialLayer mat1 = mat.getLayer(1);
		final VertexCollectorImpl buff1  = context.collectors.get(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;
		final int spriteIdCoord1 =spriteInfo.coordinate(quad.spriteId(1));

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = quad.spriteBufferU(0, 1) | (quad.spriteBufferV(0, 1) << 16);
		appendData[5] = l0 | shaderFlags1;
		appendData[6] = normalAo0;
		appendData[7] = spriteIdCoord1;

		appendData[11] = quad.spriteColor(1, 1);
		appendData[12] = quad.spriteBufferU(1, 1) | (quad.spriteBufferV(1, 1) << 16);
		appendData[13] = l1 | shaderFlags1;
		appendData[14] = normalAo1;
		appendData[15] = spriteIdCoord1;

		appendData[19] = quad.spriteColor(2, 1);
		appendData[20] = quad.spriteBufferU(2, 1) | (quad.spriteBufferV(2, 1) << 16);
		appendData[21] = l2 | shaderFlags1;
		appendData[22] = normalAo2;
		appendData[23] = spriteIdCoord1;

		appendData[27] = quad.spriteColor(3, 1);
		appendData[28] = quad.spriteBufferU(3, 1) | (quad.spriteBufferV(3, 1) << 16);
		appendData[29] = l3 | shaderFlags1;
		appendData[30] = normalAo3;
		appendData[31] = spriteIdCoord1;

		buff1.add(appendData, 32);
	}

	static void bufferQuadDirect3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt)(Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final MeshMaterial mat = quad.material().get();
		final SpriteInfoTexture spriteInfo = SpriteInfoTexture.instance();

		final MeshMaterialLayer mat0 = mat.getLayer(0);
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

		quad.transformAndAppend(0, matrix, appendData, 0);
		quad.transformAndAppend(1, matrix, appendData, 8);
		quad.transformAndAppend(2, matrix, appendData, 16);
		quad.transformAndAppend(3, matrix, appendData, 24);

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

		final int spriteIdCoord0 = spriteInfo.coordinate(quad.spriteId(0));

		appendData[3] = quad.spriteColor(0, 0);
		appendData[4] = quad.spriteBufferU(0, 0) | (quad.spriteBufferV(0, 0) << 16);
		appendData[5] = l0 | shaderFlags0;
		appendData[6] = normalAo0;
		appendData[7] = spriteIdCoord0;

		appendData[11] = quad.spriteColor(1, 0);
		appendData[12] = quad.spriteBufferU(1, 0) | (quad.spriteBufferV(1, 0) << 16);
		appendData[13] = l1 | shaderFlags0;
		appendData[14] = normalAo1;
		appendData[15] = spriteIdCoord0;

		appendData[19] = quad.spriteColor(2, 0);
		appendData[20] = quad.spriteBufferU(2, 0) | (quad.spriteBufferV(2, 0) << 16);
		appendData[21] = l2 | shaderFlags0;
		appendData[22] = normalAo2;
		appendData[23] = spriteIdCoord0;

		appendData[27] = quad.spriteColor(3, 0);
		appendData[28] = quad.spriteBufferU(3, 0) | (quad.spriteBufferV(3, 0) << 16);
		appendData[29] = l3 | shaderFlags0;
		appendData[30] = normalAo3;
		appendData[31] = spriteIdCoord0;

		buff0.add(appendData, 32);

		final MeshMaterialLayer mat1 = mat.getLayer(1);
		final VertexCollectorImpl buff1  = context.collectors.get(MaterialContext.TERRAIN, mat1);
		final int shaderFlags1 = mat1.shaderFlags << 16;
		final int spriteIdCoord1 = spriteInfo.coordinate(quad.spriteId(1));

		appendData[3] = quad.spriteColor(0, 1);
		appendData[4] = quad.spriteBufferU(0, 1) | (quad.spriteBufferV(0, 1) << 16);
		appendData[5] = l0 | shaderFlags1;
		appendData[6] = normalAo0;
		appendData[7] = spriteIdCoord1;

		appendData[11] = quad.spriteColor(1, 1);
		appendData[12] = quad.spriteBufferU(1, 1) | (quad.spriteBufferV(1, 1) << 16);
		appendData[13] = l1 | shaderFlags1;
		appendData[14] = normalAo1;
		appendData[15] = spriteIdCoord1;

		appendData[19] = quad.spriteColor(2, 1);
		appendData[20] = quad.spriteBufferU(2, 1) | (quad.spriteBufferV(2, 1) << 16);
		appendData[21] = l2 | shaderFlags1;
		appendData[22] = normalAo2;
		appendData[23] = spriteIdCoord1;

		appendData[27] = quad.spriteColor(3, 1);
		appendData[28] = quad.spriteBufferU(3, 1) | (quad.spriteBufferV(3, 1) << 16);
		appendData[29] = l3 | shaderFlags1;
		appendData[30] = normalAo3;
		appendData[31] = spriteIdCoord1;

		buff1.add(appendData, 32);

		final MeshMaterialLayer mat2 = mat.getLayer(2);
		final VertexCollectorImpl buff2  = context.collectors.get(MaterialContext.TERRAIN, mat2);
		final int shaderFlags2 = mat2.shaderFlags << 16;
		final int spriteIdCoord2 = spriteInfo.coordinate(quad.spriteId(2));

		appendData[3] = quad.spriteColor(0, 2);
		appendData[4] = quad.spriteBufferU(0, 2) | (quad.spriteBufferV(0, 2) << 16);
		appendData[5] = l0 | shaderFlags2;
		appendData[6] = normalAo0;
		appendData[7] = spriteIdCoord2;

		appendData[11] = quad.spriteColor(1, 2);
		appendData[12] = quad.spriteBufferU(1, 2) | (quad.spriteBufferV(1, 2) << 16);
		appendData[13] = l1 | shaderFlags2;
		appendData[14] = normalAo1;
		appendData[15] = spriteIdCoord2;

		appendData[19] = quad.spriteColor(2, 2);
		appendData[20] = quad.spriteBufferU(2, 2) | (quad.spriteBufferV(2, 2) << 16);
		appendData[21] = l2 | shaderFlags2;
		appendData[22] = normalAo2;
		appendData[23] = spriteIdCoord2;

		appendData[27] = quad.spriteColor(3, 2);
		appendData[28] = quad.spriteBufferU(3, 2) | (quad.spriteBufferV(3, 2) << 16);
		appendData[29] = l3 | shaderFlags2;
		appendData[30] = normalAo3;
		appendData[31] = spriteIdCoord2;

		buff2.add(appendData, 32);
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
		final int lightmap = context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
