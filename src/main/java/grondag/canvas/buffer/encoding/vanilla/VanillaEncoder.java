package grondag.canvas.buffer.encoding.vanilla;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.buffer.encoding.VertexEncoder;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;

abstract class VanillaEncoder extends VertexEncoder {
	private static int nextIndex = 0;

	VanillaEncoder(MaterialVertexFormat format) {
		super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, nextIndex++);
	}

	@Override
	public final void vertex(VertexCollectorImpl collector, double x, double y, double z) {
		collector.add((float) x);
		collector.add((float) y);
		collector.add((float) z);
	}

	@Override
	public final void vertex(VertexCollectorImpl collector, float x, float y, float z, float i, float j, float k, float l, float m, float n, int o, int p, float q, float r, float s) {
		collector.add(x);
		collector.add(y);
		collector.add(z);
		collector.color(i, j, k, l);
		collector.texture(m, n);
		collector.overlay(o);
		collector.light(p);
		collector.normal(q, r, s);
	}

	@Override
	public final void color(VertexCollectorImpl collector, int r, int g, int b, int a) {
		collector.add((r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24));
	}

	@Override
	public final void texture(VertexCollectorImpl collector, float u, float v) {
		collector.add(u);
		collector.add(v);
	}

	@Override
	public final void overlay(VertexCollectorImpl collector, int s, int t) {
		// TODO: disabled for now - needs to be controlled by format because is called when not present
		//add((s & 0xFFFF) | ((t & 0xFFFF) << 16));
	}

	@Override
	public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
		collector.add((blockLight & 0xFFFF) | ((skyLight & 0xFFFF) << 16));
	}

	@Override
	public final void normal(VertexCollectorImpl collector, float x, float y, float z) {
		collector.add(NormalHelper.packNormal(x, y, z, 1));
	}

	protected void bufferQuad1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexConsumer buff = context.consumer(quad.material());

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

	protected void bufferQuad2(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final Value mat = quad.material();
		final VertexConsumer buff1  = context.consumer(mat);
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

	protected void bufferQuad3(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final Value mat = quad.material();
		final VertexConsumer buff1 = context.consumer(mat);
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

	static void computeItemLighting(MutableQuadViewImpl quad) {
		// UGLY: for vanilla lighting need to undo diffuse shading

		// TODO: still needed in 1.16?
		ColorHelper.applyDiffuseShading(quad, true);
	}

	static void applyItemLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int lightmap = quad.material().emissive(0) ? VertexEncoder.FULL_BRIGHTNESS : context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
