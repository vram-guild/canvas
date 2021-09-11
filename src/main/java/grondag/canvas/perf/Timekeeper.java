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
import grondag.canvas.varia.GFX;

public abstract class Timekeeper {
	private static int maxTextWidth = -1;

	public enum Mode {
		CPU,
		GPU,
		CPU_GPU
	}

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
		private static final int CONTAINER_SETUP_FRAME = 0;
		private static final int GPU_SETUP_FRAME = 1;
		private static final int NUM_SETUP_FRAMES = 2;

		private long startCpu;
		private String cpuStep;
		private Object2LongOpenHashMap<String> cpuElapsed;
		private Object2LongOpenHashMap<String> gpuElapsed;
		private Group[] groups;
		private int[] gpuQueryId;
		private int frameSinceReload;
		private boolean gpuEnabled = false;

		private void reload(boolean enableGpu) {
			maxTextWidth = -1;
			frameSinceReload = -1;
			gpuEnabled = enableGpu;
		}

		@Override
		public void startFrame(ProfilerGroup group, String token) {
			cpuStep = null;

			if (frameSinceReload < NUM_SETUP_FRAMES) {
				frameSinceReload++;
			}

			switch (frameSinceReload) {
				// Setting up container is done at start of frame 0
				// This prevents multiple setup calls from config reload with multiple config vars
				case CONTAINER_SETUP_FRAME:
					final ProfilerGroup[] enumVals = ProfilerGroup.values();

					cpuElapsed = new Object2LongOpenHashMap<>();
					groups = new Group[enumVals.length];

					for (int i = 0; i < enumVals.length; i++) {
						groups[i] = new Group(enumVals[i]);
					}

					break;

				// Setting up time queries is done at start of frame 1 after all steps of each groups are populated
				case GPU_SETUP_FRAME:
					if (gpuEnabled) {
						generateQueries();
					}

					break;
			}

			swap(group, token);
		}

		@Override
		public void swap(ProfilerGroup group, String token) {
			if (cpuStep != null) {
				final long elapsed = Util.getMeasuringTimeNano() - startCpu;

				cpuElapsed.put(cpuStep, elapsed);

				if (Configurator.logRenderLagSpikes && elapsed > threshold) {
					CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", cpuStep, elapsed, threshold));
				}
			}

			cpuStep = token;
			startCpu = Util.getMeasuringTimeNano();

			if (frameSinceReload == CONTAINER_SETUP_FRAME && token != null && group != null) {
				groups[group.ordinal()].steps.add(token);
			}

			if (frameSinceReload >= GPU_SETUP_FRAME && gpuEnabled) {
				// Count end time of previous process
				GFX.glEndQuery(GFX.GL_TIME_ELAPSED);
				assert GFX.logError("Ending GPU Time Query");

				final int idIndex = getIdIndex(group, token);

				if (idIndex > -1) {
					// Count start time of current process
					GFX.glBeginQuery(GFX.GL_TIME_ELAPSED, gpuQueryId[idIndex]);
					assert GFX.logError("Beginning GPU Time Query");
				}
			}
		}

		@Override
		public void completePass() {
			swap(null, null);
		}

		public boolean populateResult() {
			if (frameSinceReload < GPU_SETUP_FRAME || !gpuEnabled) {
				return false;
			}

			int ready = 0;
			int[] temp = new int[1];

			final int numQueries = gpuQueryId.length;

			// Wait until all query result is ready (shouldn't cause infinite loop, but..?)
			while (ready < numQueries) {
				ready = 0;

				for (int i = 0; i < numQueries; i++) {
					GFX.glGetQueryObjectiv(gpuQueryId[i], GFX.GL_QUERY_RESULT_AVAILABLE, temp);
					ready += temp[0];
				}
			}

			long[] elapsed = new long[1];
			int i = 0;

			for (int j = 0; j < groups.length; j++) {
				for (String token:groups[j].steps) {
					GFX.glGetQueryObjecti64v(gpuQueryId[i], GFX.GL_QUERY_RESULT, elapsed);
					gpuElapsed.put(token, elapsed[0]);
					i++;
				}
			}

			assert GFX.logError("Populating GPU Time Query Results");

			return true;
		}

		/**
		 * Delete all query objects if exists.
		 * Make sure that this is called on reload frame and on config or pipeline reload.
		 */
		public void deleteQueries() {
			if (gpuQueryId == null) {
				return;
			}

			GFX.glDeleteQueries(gpuQueryId);
			assert GFX.logError("Deleting GPU Time Query Objects");

			gpuQueryId = null;
		}

		private void generateQueries() {
			if (gpuQueryId != null) {
				deleteQueries();
			}

			int count = 0;

			for (int i = 0; i < groups.length; i++) {
				count += groups[i].steps.size();
			}

			final int numSteps = count;

			gpuQueryId = new int[numSteps];
			gpuElapsed = new Object2LongOpenHashMap<>(numSteps);

			GFX.glGenQueries(gpuQueryId);
			assert GFX.logError("Generating GPU Time Query Objects");
		}

		private int getIdIndex(ProfilerGroup group, String token) {
			if (token == null) {
				return -1;
			}

			int idIndex = -1;
			int idOffset = 0;

			// PERF: use hash map
			for (int i = 0; i < groups.length; i++) {
				final ProfilerGroup p = groups[i].enumVal;

				if (p.equals(group)) {
					idIndex = groups[p.ordinal()].steps.indexOf(token);

					if (idIndex > -1) {
						idIndex += idOffset;
						break;
					}
				}

				idOffset += groups[p.ordinal()].steps.size();
			}

			return idIndex;
		}

		public long getCpuTime(String token) {
			return cpuElapsed.getLong(token);
		}

		public long getGpuTime(String token) {
			if (gpuEnabled && gpuElapsed != null) {
				return gpuElapsed.getLong(token);
			}

			return 0L;
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

		// make sure to always delete queries on reload
		if (instance instanceof Active) {
			((Active) instance).deleteQueries();
		}

		if (!enabled) {
			instance = DEACTIVATED;
		} else {
			if (!(instance instanceof Active)) {
				instance = new Active();
			}

			threshold = 1000000000L / Configurator.renderLagSpikeFps;

			final Active active = (Active) instance;

			active.reload(Configurator.profilerDisplayMode != Mode.CPU);
		}
	}

	public static void renderOverlay(MatrixStack ms, TextRenderer fr) {
		if (!(instance instanceof Active) || !Configurator.displayRenderProfiler) return;

		final Active active = (Active) instance;
		final float overlayScale = Configurator.profilerOverlayScale;

		ms.push();
		ms.scale(overlayScale, overlayScale, overlayScale);

		if (active.gpuEnabled) {
			active.populateResult();
		}

		if (maxTextWidth == -1) {
			final int bracketsWidth = fr.getWidth("<>");

			for (final Group group:active.groups) {
				maxTextWidth = Math.max(fr.getWidth(group.enumVal.token) + bracketsWidth, maxTextWidth);

				if (group.enumVal.level <= Configurator.profilerDetailLevel) {
					for (final String step : group.steps) {
						maxTextWidth = Math.max(fr.getWidth(step), maxTextWidth);
					}
				}
			}
		}

		int[] i = new int[]{0};
		String lastToken = null;

		for (final Group group:active.groups) {
			if (group.enumVal.level > Configurator.profilerDetailLevel) {
				long groupCpu = 0;
				long groupGpu = 0;

				for (final String step:group.steps) {
					groupCpu += active.getCpuTime(step);
					groupGpu += active.getGpuTime(step);
				}

				renderTime(String.format("<%s>", group.enumVal.token), 0, groupCpu, groupGpu, i, ms, fr);
			} else {
				for (final String step:group.steps) {
					final long cpu = active.getCpuTime(step);
					final long gpu = active.getGpuTime(step);

					if (!group.enumVal.token.equals(lastToken)) {
						final String s = String.format("<%s>", group.enumVal.token);
						renderBack(i, 0, fr.getWidth(s), 0x99000000, ms);
						renderLine(s, i, 0, 0xFFFFFFFF, ms, fr);
						lastToken = group.enumVal.token;
					}

					renderTime(String.format("%s", step), 24, cpu, gpu, i, ms, fr);
				}
			}
		}

		ms.pop();
	}

	private static void renderTime(String label, int xo, long cpu, long gpu, int[] i, MatrixStack ms, TextRenderer fr) {
		final int forecolor;
		final int backcolor;

		if (cpu <= threshold) {
			forecolor = 0xFFFFFFFF;
			backcolor = 0x99000000;
		} else {
			forecolor = 0xFFFFFF00;
			backcolor = 0x99990000;
		}

		final String l = String.format("%s", label);
		final String s = switch (Configurator.profilerDisplayMode) {
			case CPU -> String.format("C %.3f ms", cpu / 1000000f);
			case GPU -> String.format("G %.3f ms", gpu / 1000000f);
			case CPU_GPU -> String.format("C %.3f ms, G %.3f ms", cpu / 1000000f, gpu / 1000000f);
		};

		renderBack(i, xo, fr.getWidth(s) + maxTextWidth + 12, backcolor, ms);
		renderText(label, i, xo, forecolor, ms, fr);
		renderLine(s, i, xo + maxTextWidth + 12, forecolor, ms, fr);
	}

	private static void renderLine(String s, int[] i, int xo, int fcolor, MatrixStack ms, TextRenderer fr) {
		renderText(s, i, xo, fcolor, ms, fr);
		i[0]++;
	}

	private static void renderText(String s, int[] i, int xo, int fcolor, MatrixStack ms, TextRenderer fr) {
		final int k = fr.getWidth(s);
		final int m = 6 + 12 * i[0];
		fr.draw(ms, s, 6 + xo, m, fcolor);
	}

	private static void renderBack(int[] i, int xo, int w, int bcolor, MatrixStack ms) {
		final int m = 6 + 12 * i[0];
		DrawableHelper.fill(ms, 5 + xo, m - 1, 6 + xo + w, m + 9, bcolor);
	}
}
