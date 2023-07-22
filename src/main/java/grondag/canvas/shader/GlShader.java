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

package grondag.canvas.shader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import org.anarres.cpp.DefaultPreprocessorListener;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import io.vram.frex.api.config.ShaderConfig;
import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.GFX;

public class GlShader implements Shader {
	static final Pattern PATTERN = Pattern.compile("^#include\\s+(\\\"*[\\w]+:[\\w/\\.]+)[ \\t]*.*", Pattern.MULTILINE);
	private static final HashSet<String> INCLUDED = new HashSet<>();
	private static boolean isErrorNoticeComplete = false;
	private static boolean needsClearDebugOutputWarning = true;
	private static boolean needsDebugOutputWarning = true;
	private final ResourceLocation shaderSourceId;
	protected final int shaderType;
	protected final ProgramType programType;
	private String source = null;
	private int glId = -1;
	private boolean needsLoad = true;
	private boolean isErrored = false;

	public GlShader(ResourceLocation shaderSource, int shaderType, ProgramType programType) {
		shaderSourceId = shaderSource;
		this.shaderType = shaderType;
		this.programType = programType;
	}

	static void forceReloadErrors() {
		isErrorNoticeComplete = false;
	}

	@SuppressWarnings("resource")
	private static Path shaderDebugPath() {
		return Minecraft.getInstance().gameDirectory.toPath().normalize().resolve("canvas_shader_debug");
	}

	static void clearDebugSource() {
		final Path path = shaderDebugPath();

		try {
			FileUtils.deleteDirectory(path.toFile());
		} catch (final Exception e) {
			if (needsClearDebugOutputWarning) {
				CanvasMod.LOG.error(I18n.get("error.canvas.fail_clear_shader_output", path), e);
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
				glId = GFX.glCreateShader(shaderType);

				if (glId == 0) {
					glId = -1;
					isErrored = true;
					return;
				}
			}

			source = getSource();

			GFX.shaderSource(glId, new String[] { source });
			GFX.glCompileShader(glId);

			if (GFX.glGetShaderi(glId, GFX.GL_COMPILE_STATUS) == GFX.GL_FALSE) {
				isErrored = true;
				error = GFX.getShaderInfoLog(glId);

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
				GFX.glDeleteShader(glId);
				glId = -1;
			}

			if (Configurator.conciseErrors) {
				if (!isErrorNoticeComplete) {
					CanvasMod.LOG.error(I18n.get("error.canvas.fail_create_any_shader"));
					CanvasMod.displayClientError(I18n.get("error.canvas.shader_fail_client"));
					isErrorNoticeComplete = true;
				}
			} else {
				CanvasMod.LOG.error(I18n.get("error.canvas.fail_create_shader", shaderSourceId.toString(), programType.name, error));
			}

			outputDebugSource(source, error);
		} else if (Configurator.shaderDebug) {
			outputDebugSource(source, null);
		}

		// Explicitly checking CanvasGlHelper.supportsKhrDebug() to not generate the name if KHR_debug is not supported
		if (!isErrored && CanvasGlHelper.supportsKhrDebug()) {
			final int slashI = shaderSourceId.getPath().lastIndexOf('/');
			String name = slashI != -1 ? shaderSourceId.getPath().substring(slashI + 1) : shaderSourceId.getPath();

			final int dotI = name.lastIndexOf('.');

			if (dotI != -1) {
				final String extension = name.substring(dotI + 1);
				name = name.substring(0, dotI);

				if (extension.equals("vert")) {
					name = "SHA_VERT " + name;
				} else if (extension.equals("frag")) {
					name = "SHA_FRAG " + name;
				} else {
					name = "SHA " + name;
				}
			} else {
				name = "SHA " + name;
			}

			GFX.objectLabel(GFX.GL_SHADER, glId, name);
		}
	}

	protected String debugSourceString() {
		return shaderSourceId.getPath().toString().replace("/", "-").replace(":", "-");
	}

	private void outputDebugSource(String source, String error) {
		final String fileName = debugSourceString();
		final Path path = shaderDebugPath();

		File shaderDir = path.toFile();

		if (shaderDir.mkdir()) {
			CanvasMod.LOG.info("Created shader debug output folder " + shaderDir.toString());
		}

		if (error != null) {
			shaderDir = path.resolve("failed").toFile();

			if (shaderDir.mkdir()) {
				CanvasMod.LOG.info("Created shader debug output failure folder " + shaderDir.toString());
			}

			source += "\n\n///////// ERROR ////////\n" + error + "\n////////////////////////\n";
		}

		if (shaderDir.exists()) {
			try (FileWriter writer = new FileWriter(shaderDir.getAbsolutePath() + File.separator + fileName, false)) {
				writer.write(source);
			} catch (final IOException e) {
				if (needsDebugOutputWarning) {
					CanvasMod.LOG.error(I18n.get("error.canvas.fail_create_shader_output", path), e);
					needsDebugOutputWarning = false;
				}
			}
		}
	}

	private String getSource() {
		String result = source;

		if (result == null) {
			result = getCombinedShaderSource();

			if (Pipeline.config().enablePBR) {
				result = StringUtils.replace(result, "//#define PBR_ENABLED", "#define PBR_ENABLED");
			}

			if (!CanvasGlHelper.supportsArbConservativeDepth()) {
				result = StringUtils.replace(result, "#define _CV_ARB_CONSERVATIVE_DEPTH", "//#define _CV_ARB_CONSERVATIVE_DEPTH");
			}

			if (!PreReleaseShaderCompat.needsFragmentShaderStubs()) {
				result = StringUtils.replace(result, "#define _CV_FRAGMENT_COMPAT", "//#define _CV_FRAGMENT_COMPAT");
			}

			if (programType.isTerrain) {
				result = StringUtils.replace(result, "#define _CV_VERTEX_DEFAULT", "#define _CV_VERTEX_TERRAIN");
			}

			if (programType.hasVertexProgramControl) {
				result = StringUtils.replace(result, "#define PROGRAM_BY_UNIFORM", "//#define PROGRAM_BY_UNIFORM");
			}

			if (shaderType == GL21.GL_FRAGMENT_SHADER) {
				result = StringUtils.replace(result, "#define VERTEX_SHADER", "#define FRAGMENT_SHADER");
			}

			if (!Configurator.wavyGrass) {
				result = StringUtils.replace(result, "#define ANIMATED_FOLIAGE", "//#define ANIMATED_FOLIAGE");
			}

			if (programType.isDepth) {
				result = StringUtils.replace(result, "//#define DEPTH_PASS", "#define DEPTH_PASS");
			}

			if (Pipeline.shadowsEnabled()) {
				result = StringUtils.replace(result, "#define SHADOW_MAP_SIZE 1024", "#define SHADOW_MAP_SIZE " + Pipeline.skyShadowSize);
			} else {
				result = StringUtils.replace(result, "#define SHADOW_MAP_PRESENT", "//#define SHADOW_MAP_PRESENT");
				result = StringUtils.replace(result, "#define SHADOW_MAP_SIZE 1024", "//#define SHADOW_MAP_SIZE 1024");
			}

			if (Pipeline.coloredLightsEnabled()) {
				result = StringUtils.replace(result, "//#define COLORED_LIGHTS_ENABLED", "#define COLORED_LIGHTS_ENABLED");

				if (Pipeline.config().coloredLights.useOcclusionData) {
					result = StringUtils.replace(result, "//#define LIGHT_DATA_HAS_OCCLUSION", "#define LIGHT_DATA_HAS_OCCLUSION");
				}
			}

			result = StringUtils.replace(result, "#define _CV_MAX_SHADER_COUNT 0", "#define _CV_MAX_SHADER_COUNT " + MaterialConstants.MAX_SHADERS);

			// prepend GLSL version
			result = "#version " + Pipeline.config().glslVersion + "\n\n" + result;

			//if (Configurator.hdLightmaps()) {
			//	result = StringUtils.replace(result, "#define VANILLA_LIGHTING", "//#define VANILLA_LIGHTING");
			//
			//	if (Configurator.lightmapNoise) {
			//		result = StringUtils.replace(result, "//#define ENABLE_LIGHT_NOISE", "#define ENABLE_LIGHT_NOISE");
			//	}
			//}

			if (Configurator.preprocessShaderSource) {
				result = glslPreprocessSource(result);
			}

			source = result;
		}

		return result;
	}

	protected String getCombinedShaderSource() {
		final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		INCLUDED.clear();
		String result = loadShaderSource(resourceManager, shaderSourceId);
		result = preprocessSource(resourceManager, result);
		return processSourceIncludes(resourceManager, result);
	}

	protected String preprocessSource(ResourceManager resourceManager, String baseSource) {
		return baseSource;
	}

	protected static String loadShaderSource(ResourceManager resourceManager, ResourceLocation shaderSourceId) {
		String result;

		try (InputStream inputStream = resourceManager.getResource(shaderSourceId).get().open()) {
			try (Reader reader = new InputStreamReader(inputStream)) {
				result = CharStreams.toString(reader);
			}
		} catch (final FileNotFoundException | NoSuchElementException e) {
			result = Pipeline.config().configSource(shaderSourceId);

			if (result == null) {
				result = ShaderConfig.getShaderConfigSupplier(shaderSourceId).get();
			}
		} catch (final IOException e) {
			CanvasMod.LOG.warn("Unable to load shader resource " + shaderSourceId.toString() + " due to exception.", e);
			return "";
		}

		return result == null || result.isBlank() ? "" : PreReleaseShaderCompat.compatify(result, shaderSourceId);
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
				final String src = processSourceIncludes(resourceManager, loadShaderSource(resourceManager, new ResourceLocation(id)));
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
	public String typeofUniformSpec(String name) {
		final String regex = "(?m)^\\s*uniform\\s(\\w*)\\s+" + name + "\\s*;";
		final Pattern pattern = Pattern.compile(regex);
		final var matcher = pattern.matcher(getSource());

		if (matcher.find() && matcher.groupCount() > 0) {
			return matcher.group(1);
		}

		return null;
	}

	@Override
	public ResourceLocation getShaderSourceId() {
		return shaderSourceId;
	}

	private static String glslPreprocessSource(String source) {
		// The C preprocessor won't understand the #version token but
		// we need to intercept __VERSION__ used in conditional compilation.
		// GLSL won't let use define tokens starting with two underscores so
		// to intercept __VERSION__ we have to temporarily rename it.
		// It will be stripped at the end so we will need to prepend #version after
		source = StringUtils.replace(source, "#version ", "#define _CV_VERSION ");
		source = StringUtils.replace(source, "__VERSION__", "_CV_VERSION");

		@SuppressWarnings("resource")
		final Preprocessor pp = new Preprocessor();
		pp.setListener(new DefaultPreprocessorListener());
		pp.addInput(new StringLexerSource(source, true));
		//pp.addFeature(Feature.KEEPCOMMENTS);

		final StringBuilder builder = new StringBuilder();

		try {
			for (;;) {
				final Token tok = pp.token();
				if (tok.getType() == Token.EOF) break;
				builder.append(tok.getText());
			}
		} catch (final Exception e) {
			CanvasMod.LOG.error("GLSL source pre-processing failed", e);
		}

		builder.append("\n");

		source = builder.toString();

		// Enable conservative depth if available.
		// Handle before #version so that #version is always first line as required
		if (CanvasGlHelper.supportsArbConservativeDepth()) {
			source = "#extension GL_ARB_conservative_depth: enable\n" + source;
		}

		// restore GLSL version, enable conservative depth
		source = "#version " + Pipeline.config().glslVersion + "\n" + source;

		// strip commented preprocessor declarations
		source = source.replaceAll("//#.*[ \t]*[\r\n]", "\n");
		// strip leading whitepsace before newline, makes next change more reliable
		source = source.replaceAll("[ \t]*[\r\n]", "\n");
		// consolidate newlines
		source = source.replaceAll("\n{2,}", "\n\n");
		// inefficient way to remove multiple orhpaned comment blocks
		source = source.replaceAll("\\/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*\\/[\\s]+\\/\\*", "/*");
		source = source.replaceAll("\\/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*\\/[\\s]+\\/\\*", "/*");
		source = source.replaceAll("\\/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*\\/[\\s]+\\/\\*", "/*");

		return source;
	}
}
