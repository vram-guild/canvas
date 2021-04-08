package grondag.canvas.perf;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

public abstract class Timekeeper {

	private static long threshold;

	public abstract void startFrame(String token);
	public abstract void swap(String token);
	public abstract void complete();

	private static class Active extends Timekeeper {
		private long start;
		private String currentStep;
		private Object2LongOpenHashMap<String> stepElapsed;
		private ObjectArrayList<String> steps;

		private int frameSinceReload;
		// Setup is done in all steps over single frames for every reload
		// Frame 0: setup data container and list of steps
		private final int SETUP_FRAMES = 1;

		private void reload() {
			frameSinceReload = -1;
		}

		public void startFrame(String token) {
			currentStep = null;
			if (frameSinceReload < SETUP_FRAMES) {
				frameSinceReload++;
			}
			// Setting up container is done in start of frame 0
			// This prevents multiple setup calls from config reload with multiple config vars
			if (frameSinceReload == 0) {
				stepElapsed = new Object2LongOpenHashMap<>();
				steps = new ObjectArrayList<>();
			}

			swap(token);
		}

		public void swap(String token) {
			if (currentStep != null) {
				final long elapsed = Util.getMeasuringTimeNano() - start;
				stepElapsed.put(currentStep, elapsed);
				if(Configurator.logRenderLagSpikes && elapsed > threshold) {
					CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", currentStep, elapsed, threshold));
				}
			}
			if (frameSinceReload == 0 && token != null) {
				steps.add(token);
			}
			currentStep = token;
			start = Util.getMeasuringTimeNano();
		}

		public void complete() {
			swap(null);
		}
	}

	private static class Deactivated extends Timekeeper{
		public void startFrame(String token) { }
		public void swap(String token) { }
		public void complete() { }
	}

	private static final Timekeeper DEACTIVATED = new Deactivated();
	public static Timekeeper instance = DEACTIVATED;

	public static void configOrPipelineReload() {
		final boolean enabled = Configurator.displayRenderProfiler || Configurator.logRenderLagSpikes;
		if (!enabled) {
			instance = DEACTIVATED;
		} else {
			if (instance == DEACTIVATED) {
				instance = new Active();
			}
			threshold = 1000000000L / Configurator.renderLagSpikeFps;
			final Active active = (Active)instance;
			active.reload();
		}
	}

	public static void renderOverlay(MatrixStack matrices, TextRenderer fontRenderer) {
		if (instance == DEACTIVATED) return;
		if (!Configurator.displayRenderProfiler) return;

		final Active active = (Active)instance;
		final float overlayScale = Configurator.profilerOverlayScale;
		matrices.push();
		matrices.scale(overlayScale, overlayScale, overlayScale);

		int i = 0;
		for (String step:active.steps) {
			final long elapsed = active.stepElapsed.getLong(step);
			final int forecolor;
			final int backcolor;
			if (elapsed <= threshold) {
				forecolor = 0xFFFFFF;
				backcolor = 0x99000000;
			} else {
				forecolor = 0xFFFF00;
				backcolor = 0x99990000;
			}
			renderTime(step, elapsed, i++, forecolor, backcolor, matrices, fontRenderer);
		}

		matrices.pop();
	}

	private static void renderTime(String label, long time, int i, int forecolor, int backcolor, MatrixStack matrices, TextRenderer fontRenderer) {
		final String s = String.format("%s: %f ms", label, (float)time/1000000f);
		final int k = fontRenderer.getWidth(s);
		final int m = 100 + 12 * i;
		DrawableHelper.fill(matrices, 20, m - 1, 22 + k + 1, m + 9, backcolor);
		fontRenderer.draw(matrices, s, 21, m, forecolor);
	}
}
