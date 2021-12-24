/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.varia;

import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import com.mojang.blaze3d.platform.GLX;

import net.minecraft.client.Minecraft;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public class CanvasGlHelper {
	private static boolean supportsPersistentMapped = false;
	private static boolean supportsKhrDebug = false;
	private static boolean supportsArbConservativeDepth = false;

	private static String maxGlVersion = "3.2";

	public static boolean supportsPersistentMapped() {
		return supportsPersistentMapped;
	}

	public static boolean supportsKhrDebug() {
		return supportsKhrDebug;
	}

	public static boolean supportsArbConservativeDepth() {
		return supportsArbConservativeDepth;
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
		supportsArbConservativeDepth = caps.GL_ARB_conservative_depth;
		maxGlVersion = maxGlVersion(caps);

		if (Configurator.logMachineInfo) {
			logMachineInfo(caps);
		}
	}

	private static void logMachineInfo(GLCapabilities caps) {
		final Logger log = CanvasMod.LOG;
		final Minecraft client = Minecraft.getInstance();

		log.info("==================  CANVAS RENDERER DEBUG INFORMATION ==================");
		log.info(String.format(" Java: %s %dbit   Canvas: %s", System.getProperty("java.version"), client.is64Bit() ? 64 : 32, CanvasMod.versionString));
		log.info(String.format(" CPU: %s", GLX._getCpuInfo()));
		log.info(String.format(" LWJGL: %s", GLX._getLWJGLVersion()));
		log.info(String.format(" OpenGL (Reported): %s", GLX.getOpenGLVersionString()));
		log.info(String.format(" OpenGL (Available): %s", maxGlVersion));
		log.info(String.format(" glBufferStorage: %s", caps.glBufferStorage == 0 ? "N" : "Y"));
		log.info(String.format(" KHR_debug: %s", supportsKhrDebug() ? "Y" : "N"));
		log.info(String.format(" ARB_conservative_depth: %s", supportsArbConservativeDepth ? "Y" : "N"));
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
