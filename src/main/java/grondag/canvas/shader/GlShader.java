/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.shader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.Configurator.AoMode;
import grondag.canvas.Configurator.DiffuseMode;
import grondag.canvas.Configurator.FogMode;
import grondag.canvas.varia.CanvasGlHelper;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;

public class GlShader implements Shader {
	static final Pattern PATTERN = Pattern.compile("^#include\\s+(\\\"*[\\w]+:[\\w/\\.]+)[ \\t]*.*", Pattern.MULTILINE);
	private static final HashSet<String> INCLUDED = new HashSet<>();
	private static boolean isErrorNoticeComplete = false;
	private static boolean needsClearDebugOutputWarning = true;
	private static boolean needsDebugOutputWarning = true;
	private final Identifier shaderSourceId;
	protected final int shaderType;
	protected final ProgramType programType;
	private String source = null;
	private int glId = -1;
	private boolean needsLoad = true;
	private boolean isErrored = false;

	public GlShader(Identifier shaderSource, int shaderType, ProgramType programType) {
		shaderSourceId = shaderSource;
		this.shaderType = shaderType;
		this.programType = programType;
	}

	public static void forceReloadErrors() {
		isErrorNoticeComplete = false;
		clearDebugSource();
	}

	private static Path shaderDebugPath() {
		final File gameDir = FabricLoader.getInstance().getGameDirectory();

		return gameDir.toPath().normalize().resolve("canvas_shader_debug");
	}

	private static void clearDebugSource() {
		final Path path = shaderDebugPath();

		try {
			File shaderDir = path.toFile();

			if (shaderDir.exists()) {
				final File[] files = shaderDir.listFiles();

				for (final File f : files) {
					f.delete();
				}
			}

			shaderDir = path.resolve("failed").toFile();

			if (shaderDir.exists()) {
				final File[] files = shaderDir.listFiles();

				for (final File f : files) {
					f.delete();
				}

				shaderDir.delete();
			}
		} catch (final Exception e) {
			if (needsClearDebugOutputWarning) {
				CanvasMod.LOG.error(I18n.translate("error.canvas.fail_clear_shader_output", path), e);
				needsClearDebugOutputWarning = false;
			}
		}
	}

	private int glId() {
		if (needsLoad) {
			load();
		}

		return isErrored ? -1 : glId;
	}

	private void load() {
		needsLoad = false;
		isErrored = false;
		String source = null;
		String error = null;

		try {
			if (glId <= 0) {
				glId = GL21.glCreateShader(shaderType);
				if (glId == 0) {
					glId = -1;
					isErrored = true;
					return;
				}
			}

			source = getSource();

			GL21.glShaderSource(glId, source);
			GL21.glCompileShader(glId);

			if (GL21.glGetShaderi(glId, GL21.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				isErrored = true;
				error = CanvasGlHelper.getShaderInfoLog(glId);
				if (error.isEmpty()) {
					error = "Unknown OpenGL Error.";
				}
			}

		} catch (final Exception e) {
			isErrored = true;
			error = e.getMessage();
		}

		if (isErrored) {
			if (glId > 0) {
				GL21.glDeleteShader(glId);
				glId = -1;
			}

			if (Configurator.conciseErrors) {
				if (!isErrorNoticeComplete) {
					CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_any_shader"));
					isErrorNoticeComplete = true;
				}
			} else {
				CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_shader", shaderSourceId.toString(), programType.name, error));
			}
			outputDebugSource(source, error);

		} else if (Configurator.shaderDebug) {
			outputDebugSource(source, null);
		}
	}

	protected String debugSourceString() {
		return "-" + shaderSourceId.toString().replace("/", "-").replace(":", "-");
	}

	private void outputDebugSource(String source, String error) {
		final String fileName = programType.name + "-" + debugSourceString();
		final Path path = shaderDebugPath();

		File shaderDir = path.toFile();

		if (!shaderDir.exists()) {
			shaderDir.mkdir();
			CanvasMod.LOG.info("Created shader debug output folder" + shaderDir.toString());
		}

		if (error != null) {
			shaderDir = path.resolve("failed").toFile();

			if (!shaderDir.exists()) {
				shaderDir.mkdir();
				CanvasMod.LOG.info("Created shader debug output failure folder" + shaderDir.toString());
			}

			source += "\n\n///////// ERROR ////////\n" + error + "\n////////////////////////\n";
		}

		if (shaderDir.exists()) {
			try (FileWriter writer = new FileWriter(shaderDir.getAbsolutePath() + File.separator + fileName, false)) {
				writer.write(source);
				writer.close();
			} catch (final IOException e) {
				if (needsDebugOutputWarning) {
					CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_shader_output", path), e);
					needsDebugOutputWarning = false;
				}
			}
		}
	}

	private String getSource() {
		String result = source;

		if (result == null) {
			result = getCombinedShaderSource();

			if (shaderType == GL21.GL_FRAGMENT_SHADER) {
				result = StringUtils.replace(result, "#define SHADER_TYPE SHADER_TYPE_VERTEX", "#define SHADER_TYPE SHADER_TYPE_FRAGMENT");
			}

			if (!Configurator.wavyGrass) {
				result = StringUtils.replace(result, "#define ANIMATED_FOLIAGE", "//#define ANIMATED_FOLIAGE");
			}

			if (Configurator.fogMode != FogMode.VANILLA) {
				result = StringUtils.replace(result, "#define _CV_FOG_CONFIG _CV_FOG_CONFIG_VANILLA",
					"#define _CV_FOG_CONFIG _CV_FOG_CONFIG_" + Configurator.fogMode.name());
			}

			if (Configurator.enableBloom) {
				result = StringUtils.replace(result, "#define TARGET_EXTRAS -1", "#define TARGET_EXTRAS 1");
			}

			if (Configurator.hdLightmaps()) {
				result = StringUtils.replace(result, "#define VANILLA_LIGHTING", "//#define VANILLA_LIGHTING");

				if (Configurator.lightmapNoise) {
					result = StringUtils.replace(result, "//#define ENABLE_LIGHT_NOISE", "#define ENABLE_LIGHT_NOISE");
				}
			}

			if (!MinecraftClient.isAmbientOcclusionEnabled()) {
				// disable ao for particles or if disabled by player
				result = StringUtils.replace(result, "#define AO_SHADING_MODE AO_MODE_NORMAL",
					"#define AO_SHADING_MODE AO_MODE_" + AoMode.NONE.name());
			} else if (Configurator.aoShadingMode != AoMode.NORMAL) {
				result = StringUtils.replace(result, "#define AO_SHADING_MODE AO_MODE_NORMAL",
					"#define AO_SHADING_MODE AO_MODE_" + Configurator.aoShadingMode.name());
			}

			if (Configurator.diffuseShadingMode != DiffuseMode.NORMAL) {
				result = StringUtils.replace(result, "#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_NORMAL",
					"#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_" + Configurator.diffuseShadingMode.name());
			}

			if (CanvasGlHelper.useGpuShader4()) {
				result = StringUtils.replace(result, "//#define USE_FLAT_VARYING", "#define USE_FLAT_VARYING");
			} else {
				result = StringUtils.replace(result, "#extension GL_EXT_gpu_shader4 : enable", "");
			}

			source = result;
		}

		return result;
	}

	private String getCombinedShaderSource() {
		final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
		INCLUDED.clear();
		String result = loadShaderSource(resourceManager, shaderSourceId);
		result = preprocessSource(resourceManager, result);
		return processSourceIncludes(resourceManager, result);
	}

	protected String preprocessSource(ResourceManager resourceManager, String baseSource) {
		return baseSource;
	}

	protected static String loadShaderSource(ResourceManager resourceManager, Identifier shaderSourceId) {
		try (Resource resource = resourceManager.getResource(shaderSourceId)) {
			try (Reader reader = new InputStreamReader(resource.getInputStream())) {
				return CharStreams.toString(reader);
			}
		} catch (final FileNotFoundException e) {
			CanvasMod.LOG.warn("Unable to load shader resource " + shaderSourceId.toString() + ". File was not found.");
			return "";
		} catch (final IOException e) {
			CanvasMod.LOG.warn("Unable to load shader resource " + shaderSourceId.toString() + " due to exception.", e);
			return "";
		}
	}

	private String processSourceIncludes(ResourceManager resourceManager, String source) {
		final Matcher m = PATTERN.matcher(source);

		while (m.find()) {
			// allow quoted arguments to #include for nicer IDE support
			final String id = StringUtils.replace(m.group(1), "\"", "");

			if (INCLUDED.contains(id)) {
				source = StringUtils.replace(source, m.group(0), "");
			} else {
				INCLUDED.add(id);
				final String src = processSourceIncludes(resourceManager, loadShaderSource(resourceManager, new Identifier(id)));
				source = StringUtils.replace(source, m.group(0), src, 1);
			}
		}

		return source;
	}

	/**
	 * Call after render / resource refresh to force shader reload.
	 */
	@Override
	public final void forceReload() {
		needsLoad = true;
		source = null;
	}

	@Override
	public boolean attach(int program) {
		final int glId = glId();

		if (glId <= 0) {
			return false;
		}

		GL21.glAttachShader(program, glId);
		return true;
	}

	@Override
	public boolean containsUniformSpec(String type, String name) {
		final String regex = "(?m)^\\s*uniform\\s+" + type + "\\s+" + name + "\\s*;";
		final Pattern pattern = Pattern.compile(regex);
		return pattern.matcher(getSource()).find();
	}

	@Override
	public Identifier getShaderSourceId() {
		return shaderSourceId;
	}
}
