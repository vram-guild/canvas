package grondag.canvas.buffer.encoding;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;

abstract class VanillaEncoder extends VertexEncoder {
	VanillaEncoder() {
		super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS);
	}

	protected void bufferQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Vector4f transformVector = context.transformVector;
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexConsumer buff = context.consumer(quad);

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

	/** handles block color and red-blue swizzle, common to all renders. */
	protected void colorizeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
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
	public void vertex(VertexCollectorImpl collector, double x, double y, double z) {
		collector.add((float) x);
		collector.add((float) y);
		collector.add((float) z);
	}

	@Override
	public void vertex(VertexCollectorImpl collector, float x, float y, float z, float i, float j, float k, float l, float m, float n, int o, int p, float q, float r, float s) {
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
	public void color(VertexCollectorImpl collector, int r, int g, int b, int a) {
		collector.add((r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24));
	}

	@Override
	public void texture(VertexCollectorImpl collector, float u, float v) {
		collector.add(u);
		collector.add(v);
	}

	@Override
	public void overlay(VertexCollectorImpl collector, int s, int t) {
		// TODO: disabled for now - needs to be controlled by format because is called when not present
		//add((s & 0xFFFF) | ((t & 0xFFFF) << 16));
	}

	@Override
	public void light(VertexCollectorImpl collector, int blockLight, int skyLight) {
		collector.add((blockLight & 0xFFFF) | ((skyLight & 0xFFFF) << 16));
	}

	@Override
	public void normal(VertexCollectorImpl collector, float x, float y, float z) {
		collector.add(NormalHelper.packNormal(x, y, z, 1));
	}
}
