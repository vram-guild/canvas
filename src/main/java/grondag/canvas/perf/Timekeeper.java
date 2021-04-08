package grondag.canvas.perf;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

public class Timekeeper {
	private static long start;
	private static String currentStep;
	private static Object2LongOpenHashMap<String> stepElapsed;
	private static ObjectArrayList<String> steps;

	private static int frameSincePipelineReload;
	private static final int CONFIG_FRAMES = 1;

	public static void pipelineReload() {
		stepElapsed = new Object2LongOpenHashMap<>();
		steps = new ObjectArrayList<>();
		frameSincePipelineReload = -1;
	}

	public static void startFrame() {
		currentStep = null;
		if(frameSincePipelineReload < CONFIG_FRAMES) {
			frameSincePipelineReload++;
		}
	}

	public static void swap(String token) {
		if (currentStep != null) {
			stepElapsed.put(currentStep, Util.getMeasuringTimeNano() - start);
		}
		if (frameSincePipelineReload == 0 && token != null) {
			steps.add(token);
		}
		currentStep = token;
		start = Util.getMeasuringTimeNano();
	}

	public static void complete() {
		swap(null);
		if (Configurator.logRenderLagSpikes) {
			logRenderLagSpikes();
		}
	}

	private static void logRenderLagSpikes() {
		final long threshold = 1000000000L / Configurator.renderLagSpikeFps;
		for (String step:steps) {
			final long elapsed = stepElapsed.getLong(step);
			if(elapsed > threshold) {
				CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", step, elapsed, threshold));
			}
		}
	}

	public static void renderOverlay(MatrixStack matrices, TextRenderer fontRenderer) {
		if(!Configurator.displayRenderProfiler) return;
		final float overlayScale = Configurator.profilerOverlayScale;
		matrices.push();
		matrices.scale(overlayScale, overlayScale, overlayScale);

		int i = 0;
		for (String step:steps) {
			renderTime(step, stepElapsed.getLong(step), i++, matrices, fontRenderer);
		}

		matrices.pop();
	}

	final static private int render_forecolor = 0xFFFFFF;
	final static private int render_backcolor = 0x99000000;
	private static void renderTime(String label, long time, int i, MatrixStack matrices, TextRenderer fontRenderer) {
		final String s = String.format("%s: %f ms", label, (float)time/1000000f);
		final int k = fontRenderer.getWidth(s);
		final int m = 100 + 12 * i;
		DrawableHelper.fill(matrices, 20, m - 1, 22 + k + 1, m + 9, render_backcolor);
		fontRenderer.draw(matrices, s, 21, m, render_forecolor);
	}
}
