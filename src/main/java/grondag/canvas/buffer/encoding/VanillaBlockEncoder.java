package grondag.canvas.buffer.encoding;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;

public class VanillaBlockEncoder extends VanillaEncoder {
	VanillaBlockEncoder(int index) {
		super(index);
	}

	@Override
	public void encodeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		if (!quad.material().disableAo(0) && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.aoCalc().compute(quad);
		} else {
			final int brightness = context.flatBrightness(quad);
			quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), brightness));
			quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), brightness));
			quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), brightness));
			quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), brightness));
		}

		colorizeQuad(quad, context);
		bufferQuad(quad, context);
	}
}
