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

package grondag.canvas.varia;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.GlSymbolLookup;

public class CanvasGlHelper {
	private static int attributeEnabledCount = 0;

	public static void init() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasGlHelper static init");
		}

		final GLCapabilities caps = GL.getCapabilities();

		if (Configurator.logMachineInfo) {
			logMachineInfo(caps);
		}
	}

	private static void logMachineInfo(GLCapabilities caps) {
		final Logger log = CanvasMod.LOG;
		final MinecraftClient client = MinecraftClient.getInstance();

		log.info("==================  CANVAS RENDERER DEBUG INFORMATION ==================");
		log.info(String.format(" Java: %s %dbit   Canvas: %s", System.getProperty("java.version"), client.is64Bit() ? 64 : 32,
				FabricLoader.getInstance().getModContainer(CanvasMod.MODID).get().getMetadata().getVersion()));
		log.info(String.format(" CPU: %s", GLX._getCpuInfo()));
		log.info(String.format(" LWJGL: %s", GLX._getLWJGLVersion()));
		log.info(String.format(" OpenGL: %s", GLX.getOpenGLVersionString()));
		log.info(" (This message can be disabled by configuring logMachineInfo = false.)");
		log.info("========================================================================");
	}

	/**
	 * Disables generic vertex attributes.
	 * Use after calling {@link #enableAttributesVao(int)}
	 */
	public static void disableAttributesVao(int enabledCount) {
		for (int i = 1; i <= enabledCount; i++) {
			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: glDisableVertexAttribArray(%d)", i));
			}

			GL46C.glDisableVertexAttribArray(i);
			assert CanvasGlHelper.checkError();
		}
	}

	/**
	 * Like {@link CanvasGlHelper#enableAttributes(int)} but enables all attributes
	 * regardless of prior state. Tracking state for {@link CanvasGlHelper#enableAttributes(int)}
	 * remains unchanged. Used to initialize VAO state
	 */
	public static void enableAttributesVao(int enabledCount) {
		for (int i = 0; i < enabledCount; i++) {
			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: glEnableVertexAttribArray(%d)", i));
			}

			GL46C.glEnableVertexAttribArray(i);
			assert CanvasGlHelper.checkError();
		}
	}

	/**
	 * Enables the given number of generic vertex attributes if not already enabled.
	 * Using 1-based numbering for attribute slots because GL (on my machine at
	 * least) not liking slot 0.
	 *
	 * @param enabledCount Number of needed attributes.
	 */
	public static void enableAttributes(int enabledCount) {
		if (enabledCount > attributeEnabledCount) {
			while (enabledCount > attributeEnabledCount) {
				if (Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glEnableVertexAttribArray(%d)", attributeEnabledCount + 1));
				}

				assert CanvasGlHelper.checkError();
				GL46C.glEnableVertexAttribArray(++attributeEnabledCount);
				assert CanvasGlHelper.checkError();
			}
		} else if (enabledCount < attributeEnabledCount) {
			while (enabledCount < attributeEnabledCount) {
				if (Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glDisableVertexAttribArray(%d)", attributeEnabledCount));
				}

				GL46C.glDisableVertexAttribArray(attributeEnabledCount--);
				assert CanvasGlHelper.checkError();
			}
		}
	}

	public static String getProgramInfoLog(int obj) {
		return GL46C.glGetProgramInfoLog(obj, GL46C.glGetProgrami(obj, GL46C.GL_INFO_LOG_LENGTH));
	}

	public static String getShaderInfoLog(int obj) {
		return GL46C.glGetShaderInfoLog(obj, GL46C.glGetShaderi(obj, GL46C.GL_INFO_LOG_LENGTH));
	}

	public static boolean checkError() {
		final int error = GlStateManager.getError();

		if (error == 0) {
			return true;
		} else {
			CanvasMod.LOG.warn(String.format("OpenGL Error detected: %s (%d)", GlSymbolLookup.reverseLookup(error), error));
			//WIP2: put back to false
			return true;
		}
	}
}
