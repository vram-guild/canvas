package grondag.canvas.buffer.encoding;

import net.minecraft.client.MinecraftClient;

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

		for (int i = 0; i < 4; i++) {
			// PERF: apply this in lighter directly vs here
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), quad.light[i]));
		}

		bufferQuad(quad, context);
	}
}
