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

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;

public class CanvasGlHelper {
	static boolean useVboArb;
	private static boolean vaoEnabled = false;
	private static boolean useVaoArb = false;
	private static boolean useGpuShader4 = false;
	private static int attributeEnabledCount = 0;

	public static void init() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasGlHelper static init");
		}

		final GLCapabilities caps = GL.getCapabilities();
		useVboArb = !caps.OpenGL15 && caps.GL_ARB_vertex_buffer_object;
		vaoEnabled = caps.GL_ARB_vertex_array_object || caps.OpenGL30;
		useVaoArb = !caps.OpenGL30 && caps.GL_ARB_vertex_array_object;
		useGpuShader4 = caps.GL_EXT_gpu_shader4;

		if (Configurator.logMachineInfo) {
			logMachineInfo(caps);
		}
	}

	private static void logMachineInfo(GLCapabilities caps) {
		final Logger log = CanvasMod.LOG;
		final MinecraftClient client = MinecraftClient.getInstance();

		log.info("==================  CANVAS RENDERER DEBUG INFORMATION ==================");
		log.info(String.format(" Java: %s %dbit", System.getProperty("java.version"), client.is64Bit() ? 64 : 32));
		log.info(String.format(" CPU: %s", GLX._getCpuInfo()));
		log.info(String.format(" GPU: %s  %s", GLX._getCapsString(), GLX._getLWJGLVersion()));
		log.info(String.format(" OpenGL: %s", GLX.getOpenGLVersionString()));
		log.info(String.format(
				" GpuShader4: %s  VboArb: %s  VaoEnabled: %s  VaoArb: %s",
				useGpuShader4 ? "Y" : "N",
						useVboArb ? "Y" : "N",
								vaoEnabled ? "Y" : "N",
										useVaoArb ? "Y" : "N"));
		log.info(" (This message can be disabled by configuring logMachineInfo = false.)");
		log.info("========================================================================");
	}

	public static boolean useGpuShader4() {
		return useGpuShader4;
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

			GL20.glDisableVertexAttribArray(i);
		}
	}

	/**
	 * Like {@link CanvasGlHelper#enableAttributes(int)} but enables all attributes
	 * regardless of prior state. Tracking state for {@link CanvasGlHelper#enableAttributes(int)}
	 * remains unchanged. Used to initialize VAO state
	 */
	public static void enableAttributesVao(int enabledCount) {
		for (int i = 1; i <= enabledCount; i++) {
			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: glEnableVertexAttribArray(%d)", i));
			}

			GL20.glEnableVertexAttribArray(i);
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

				GL20.glEnableVertexAttribArray(++attributeEnabledCount);
			}
		} else if (enabledCount < attributeEnabledCount) {
			while (enabledCount < attributeEnabledCount) {
				if (Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glDisableVertexAttribArray(%d)", attributeEnabledCount));
				}

				GL20.glDisableVertexAttribArray(attributeEnabledCount--);
			}
		}
	}

	public static String getProgramInfoLog(int obj) {
		return GL21.glGetProgramInfoLog(obj, GL21.glGetProgrami(obj, GL21.GL_INFO_LOG_LENGTH));
	}

	public static String getShaderInfoLog(int obj) {
		return GL21.glGetShaderInfoLog(obj, GL21.glGetShaderi(obj, GL21.GL_INFO_LOG_LENGTH));
	}

	public static boolean isVaoEnabled() {
		return vaoEnabled && Configurator.enableVao();
	}

	public static void glGenVertexArrays(IntBuffer arrays) {
		if (useVaoArb) {
			ARBVertexArrayObject.glGenVertexArrays(arrays);
		} else {
			GL30.glGenVertexArrays(arrays);
		}
	}

	public static void glBindVertexArray(int vaoBufferId) {
		if (useVaoArb) {
			ARBVertexArrayObject.glBindVertexArray(vaoBufferId);
		} else {
			GL30.glBindVertexArray(vaoBufferId);
		}
	}

	public static boolean checkError() {
		final int error = GlStateManager.getError();

		if (error == 0) {
			return true;
		} else {
			CanvasMod.LOG.warn("OpenGL Error detected: " + error);
			return false;
		}
	}
}
