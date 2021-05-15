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

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL46;

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
		protected Group[] groups;
		protected int frameSinceReload;

		// Setup is done in all steps over single frames for every reload
		// Frame 0: setup data container and list of steps
		protected int getSetupFrames() {
			return 1;
		}

		private void reload() {
			frameSinceReload = -1;
		}

		@Override
		public void startFrame(ProfilerGroup group, String token) {
			currentStep = null;

			if (frameSinceReload < getSetupFrames()) {
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

	private static class ActiveGPU extends Active {
		private int numProcesses;
		private boolean prevIsProcess;
		private int prevEndIndex;
		private Object2LongOpenHashMap<String> gpuElapsed;

		// Even indices are startTime, odd indices are endTime
		private int[] queryIDs;

		// Frame 1: setup gl query objects
		@Override
		protected int getSetupFrames() {
			return 2;
		}

		private static ProfilerGroup[] PROCESS_GROUPS = new ProfilerGroup[] {
			ProfilerGroup.BeforeWorld,
			ProfilerGroup.Fabulous,
			ProfilerGroup.AfterHand
		};

		public boolean populateResult() {

			if (frameSinceReload < 1 ) return false;

			int ready = 0;
			int[] temp = new int[1];
			while (ready < numProcesses) {
				ready = 0;
				for (int i = 0; i < numProcesses; i++) {
					GL46.glGetQueryObjectiv(queryIDs[i*2], GL15.GL_QUERY_RESULT_AVAILABLE, temp);
					if (temp[0] == 0) break;
					GL46.glGetQueryObjectiv(queryIDs[i*2+1], GL15.GL_QUERY_RESULT_AVAILABLE, temp);
					ready += temp[0];
				}
			}
			long[] start = new long[1];
			long[] end = new long[1];
			int i = 0;
			for(ProfilerGroup p:PROCESS_GROUPS) {
				for (String token:groups[p.ordinal()].steps) {
					GL46.glGetQueryObjecti64v(queryIDs[i*2], GL15.GL_QUERY_RESULT, start);
					GL46.glGetQueryObjecti64v(queryIDs[i*2+1], GL15.GL_QUERY_RESULT, end);
					gpuElapsed.put(token, end[0] - start[0]);
					i++;
				}
			}
			assert GlStateManager.getError() == 0;

			return true;
		}

		@Override
		public void startFrame(ProfilerGroup group, String token) {
			super.startFrame(group, token);
			if (frameSinceReload == 1) {
				if (queryIDs != null) {
					deleteQueries();
				}

				int count = 0;
				for (ProfilerGroup p:PROCESS_GROUPS) {
					count += groups[p.ordinal()].steps.size();
				}

				numProcesses = count;
				prevIsProcess = false;
				queryIDs = new int[numProcesses * 2];
				gpuElapsed = new Object2LongOpenHashMap<>(numProcesses);

				GL46.glGenQueries(queryIDs);
				assert GlStateManager.getError() == 0;
			}
		}

		@Override
		public void swap(ProfilerGroup group, String token) {
			super.swap(group, token);

			if (frameSinceReload < 1 ) return;

			if (prevIsProcess) {
				// Count end time of previous process
				GL46.glQueryCounter(queryIDs[prevEndIndex], GL33.GL_TIMESTAMP);
				assert GlStateManager.getError() == 0;
			}

			final int startIndex = getStartIndex(group, token);

			if (startIndex > -1) {
				// Count start time of current process
				GL46.glQueryCounter(queryIDs[startIndex], GL33.GL_TIMESTAMP);
				assert GlStateManager.getError() == 0;

				prevIsProcess = true;
				prevEndIndex = startIndex + 1;
			} else {
				prevIsProcess = false;
			}
		}

		private int getStartIndex(ProfilerGroup group, String token) {
			if (token == null) {
				return -1;
			}

			int startIndex = -1;
			int startOffset = 0;

			for (ProfilerGroup p:PROCESS_GROUPS) {
				if (p.equals(group)) {
					startIndex = groups[p.ordinal()].steps.indexOf(token);

					if (startIndex > -1) {
						startIndex += startOffset;
						startIndex *= 2;
						break;
					}
				}
				startOffset += groups[p.ordinal()].steps.size();
			}

			return startIndex;
		}

		/**
		 * Delete all query objects if exists.
		 * Make sure that this is called on reload frame and on config or pipeline reload.
		 */
		public void deleteQueries() {
			if (queryIDs == null) {
				return;
			}

			GL46.glDeleteQueries(queryIDs);
			assert GlStateManager.getError() == 0;

			queryIDs = null;
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

		// always delete queries on reload
		if (instance instanceof ActiveGPU) {
			((ActiveGPU) instance).deleteQueries();
		}

		if (!enabled) {
			instance = DEACTIVATED;
		} else {
			final boolean gpuEnabled = Configurator.profileProcessShaders;

			if (gpuEnabled) {
				if (!(instance instanceof ActiveGPU)) {
					instance = new ActiveGPU();
				}
			} else if (instance == DEACTIVATED || instance instanceof ActiveGPU) {
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

		if (active instanceof ActiveGPU) {
			final ActiveGPU activeGPU = (ActiveGPU) active;

			if (activeGPU.populateResult()) {

				for (ProfilerGroup p : ActiveGPU.PROCESS_GROUPS) {
					for (String step : activeGPU.groups[p.ordinal()].steps) {
						final long elapsed = activeGPU.gpuElapsed.getLong(step);
						renderTime(String.format("gpuTime [%s] %s", p.token, step), elapsed, i++, matrices, fontRenderer);
					}
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
