/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.perf;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public abstract class Timekeeper {
	public enum ProfilerGroup {
		GameRendererSetup("GameRenderer_Setup", 2),
		BeforeWorld("Before World", 1),
		StartWorld("Start World", 2),
		ShadowMap("Shadow Map", 2),
		EndWorld("End World", 2),
		Fabulous("Fabulous", 1),
		AfterFabulous("After Fabulous", 2),
		AfterHand("After Hand", 1);

		public final String token;
		public final int level;
		ProfilerGroup(String token, int level) {
			this.token = token;
			this.level = level;
		}
	}

	private static class Group {
		private final ProfilerGroup enumVal;
		private final ObjectArrayList<String> steps;

		Group(ProfilerGroup group) {
			enumVal = group;
			steps = new ObjectArrayList<>();
		}
	}

	private static long threshold;

	public abstract void startFrame(ProfilerGroup group, String token);
	public abstract void swap(ProfilerGroup group, String token);
	public abstract void completePass();

	private static class Active extends Timekeeper {
		private long start;
		private String currentStep;
		private Object2LongOpenHashMap<String> stepElapsed;
		private Group[] groups;

		private int frameSinceReload;
		// Setup is done in all steps over single frames for every reload
		// Frame 0: setup data container and list of steps
		private final int SETUP_FRAMES = 1;

		private void reload() {
			frameSinceReload = -1;
		}

		@Override
		public void startFrame(ProfilerGroup group, String token) {
			currentStep = null;

			if (frameSinceReload < SETUP_FRAMES) {
				frameSinceReload++;
			}

			// Setting up container is done in start of frame 0
			// This prevents multiple setup calls from config reload with multiple config vars
			if (frameSinceReload == 0) {
				stepElapsed = new Object2LongOpenHashMap<>();
				final ProfilerGroup[] enumVals = ProfilerGroup.values();
				groups = new Group[enumVals.length];

				for (int i = 0; i < enumVals.length; i++) {
					groups[i] = new Group(enumVals[i]);
				}
			}

			swap(group, token);
		}

		@Override
		public void swap(ProfilerGroup group, String token) {
			if (currentStep != null) {
				final long elapsed = Util.getMeasuringTimeNano() - start;
				stepElapsed.put(currentStep, elapsed);

				if (Configurator.logRenderLagSpikes && elapsed > threshold) {
					CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", currentStep, elapsed, threshold));
				}
			}

			if (frameSinceReload == 0 && token != null && group != null) {
				groups[group.ordinal()].steps.add(token);
			}

			currentStep = token;

			start = Util.getMeasuringTimeNano();
		}

		@Override
		public void completePass() {
			swap(null, null);
		}
	}

	private static class Deactivated extends Timekeeper {
		@Override
		public void startFrame(ProfilerGroup group, String token) { }
		@Override
		public void swap(ProfilerGroup group, String token) { }
		@Override
		public void completePass() { }
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
			final Active active = (Active) instance;
			active.reload();
		}
	}

	public static void renderOverlay(MatrixStack matrices, TextRenderer fontRenderer) {
		if (instance == DEACTIVATED) return;
		if (!Configurator.displayRenderProfiler) return;

		final Active active = (Active) instance;
		final float overlayScale = Configurator.profilerOverlayScale;
		matrices.push();
		matrices.scale(overlayScale, overlayScale, overlayScale);

		int i = 0;

		for (final Group group:active.groups) {
			if (group.enumVal.level > Configurator.profilerDetailLevel) {
				long groupElapsed = 0;

				for (final String step:group.steps) {
					groupElapsed += active.stepElapsed.getLong(step);
				}

				renderTime(String.format("<%s>", group.enumVal.token), groupElapsed, i++, matrices, fontRenderer);
			} else {
				for (final String step:group.steps) {
					final long elapsed = active.stepElapsed.getLong(step);
					renderTime(String.format("[%s] %s", group.enumVal.token, step), elapsed, i++, matrices, fontRenderer);
				}
			}
		}

		matrices.pop();
	}

	private static void renderTime(String label, long time, int i, MatrixStack matrices, TextRenderer fontRenderer) {
		final int forecolor;
		final int backcolor;

		if (time <= threshold) {
			forecolor = 0xFFFFFF;
			backcolor = 0x99000000;
		} else {
			forecolor = 0xFFFF00;
			backcolor = 0x99990000;
		}

		final String s = String.format("%s: %f ms", label, time/1000000f);
		final int k = fontRenderer.getWidth(s);
		final int m = 100 + 12 * i;
		DrawableHelper.fill(matrices, 20, m - 1, 22 + k + 1, m + 9, backcolor);
		fontRenderer.draw(matrices, s, 21, m, forecolor);
	}
}
