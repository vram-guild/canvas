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
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public class CanvasGlHelper {
	private static boolean supportsPersistentMapped = false;
	private static boolean supportsKhrDebug = false;

	private static String maxGlVersion = "3.2";

	public static boolean supportsPersistentMapped() {
		return supportsPersistentMapped;
	}

	public static boolean supportsKhrDebug() {
		return supportsKhrDebug;
	}

	public static String maxGlVersion() {
		return maxGlVersion;
	}

	public static void init() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasGlHelper static init");
		}

		final GLCapabilities caps = GL.getCapabilities();
		supportsPersistentMapped = caps.glBufferStorage != 0;
		supportsKhrDebug = caps.GL_KHR_debug;
		maxGlVersion = maxGlVersion(caps);

		if (Configurator.logMachineInfo) {
			logMachineInfo(caps);
		}
	}

	private static void logMachineInfo(GLCapabilities caps) {
		final Logger log = CanvasMod.LOG;
		final MinecraftClient client = MinecraftClient.getInstance();

		log.info("==================  CANVAS RENDERER DEBUG INFORMATION ==================");
		log.info(String.format(" Java: %s %dbit   Canvas: %s", System.getProperty("java.version"), client.is64Bit() ? 64 : 32, CanvasMod.versionString));
		log.info(String.format(" CPU: %s", GLX._getCpuInfo()));
		log.info(String.format(" LWJGL: %s", GLX._getLWJGLVersion()));
		log.info(String.format(" OpenGL (Reported): %s", GLX.getOpenGLVersionString()));
		log.info(String.format(" OpenGL (Available): %s", maxGlVersion));
		log.info(String.format(" glBufferStorage: %s", caps.glBufferStorage == 0 ? "N" : "Y"));
		log.info(String.format(" KHR_debug: %s", supportsKhrDebug() ? "Y" : "N"));
		log.info(" (This message can be disabled by configuring logMachineInfo = false.)");
		log.info("========================================================================");
	}

	private static String maxGlVersion(GLCapabilities caps) {
		if (caps.OpenGL46) {
			return "4.6";
		} else if (caps.OpenGL45) {
			return "4.5";
		} else if (caps.OpenGL44) {
			return "4.4";
		} else if (caps.OpenGL43) {
			return "4.3";
		} else if (caps.OpenGL42) {
			return "4.2";
		} else if (caps.OpenGL41) {
			return "4.1";
		} else if (caps.OpenGL40) {
			return "4.0";
		} else if (caps.OpenGL33) {
			return "3.3";
		} else {
			return "3.2";
		}
	}
}
