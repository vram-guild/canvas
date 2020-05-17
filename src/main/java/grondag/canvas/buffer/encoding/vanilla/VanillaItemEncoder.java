package grondag.canvas.buffer.encoding.vanilla;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.encoding.VertexEncoder;

class VanillaItemEncoder extends VanillaEncoder {
	VanillaItemEncoder(int index) {
		super(index);
	}

	@Override
	public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		// needs to happen before offsets are applied
		computeItemLighting(quad);

		colorizeQuad(quad, context);

		applyItemLighting(quad, context);

		bufferQuad(quad, context);
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	@Override
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

	public void computeItemLighting(MutableQuadViewImpl quad) {
		// UGLY: for vanilla lighting need to undo diffuse shading
		ColorHelper.applyDiffuseShading(quad, true);
	}

	public void applyItemLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int lightmap = quad.material().emissive(0) ? VertexEncoder.FULL_BRIGHTNESS : context.brightness();

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
		}
	}
}
