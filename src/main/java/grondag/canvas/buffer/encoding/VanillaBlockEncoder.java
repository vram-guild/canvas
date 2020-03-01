package grondag.canvas.buffer.encoding;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;

public class VanillaBlockEncoder extends VanillaEncoder {
	@Override
	public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		if (!quad.material().disableAo(0) && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.aoCalc().compute(quad);
		}

		colorizeQuad(quad, context);

		applyBlockLighting(quad, context);

		bufferQuad(quad, context);
	}

	public void applyBlockLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final RenderMaterialImpl.Value mat = quad.material();

		// TODO: handle multiple
		final int textureIndex = 0;

		if (context.defaultAo() && !mat.disableAo(textureIndex)) {
			if (mat.emissive(textureIndex)) {
				tesselateSmoothEmissive(quad);
			} else {
				tesselateSmooth(quad);
			}
		} else {
			if (mat.emissive(textureIndex)) {
				tesselateFlatEmissive(quad);
			} else {
				tesselateFlat(quad, context);
			}
		}
	}

	// routines below have a bit of copy-paste code reuse to avoid conditional execution inside a hot loop

	/** for non-emissive mesh quads and all fallback quads with smooth lighting. */
	private void tesselateSmooth(MutableQuadViewImpl q) {
		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), q.light[i]));
		}
	}

	/** for emissive mesh quads with smooth lighting. */
	private void tesselateSmoothEmissive(MutableQuadViewImpl q) {
		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, FULL_BRIGHTNESS);
		}
	}

	/** for non-emissive mesh quads and all fallback quads with flat lighting. */
	private void tesselateFlat(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int brightness = context.flatBrightness(quad);

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
		}
	}

	/** for emissive mesh quads with flat lighting. */
	private void tesselateFlatEmissive(MutableQuadViewImpl quad) {
		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, FULL_BRIGHTNESS);
		}
	}
}
