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

package grondag.canvas.shader;

import static org.lwjgl.system.MemoryStack.stackGet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import org.anarres.cpp.DefaultPreprocessorListener;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import io.vram.frex.api.config.ShaderConfig;

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

	public static void forceReloadErrors() {
		isErrorNoticeComplete = false;
		clearDebugSource();
	}

	@SuppressWarnings("resource")
	private static Path shaderDebugPath() {
		return Minecraft.getInstance().gameDirectory.toPath().normalize().resolve("canvas_shader_debug");
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

			safeShaderSource(glId, source);
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

	/**
	 * Identical in function to {@link GL20C#glShaderSource(int, CharSequence)} but
	 * passes a null pointer for string length to force the driver to rely on the null
	 * terminator for string length.  This is a workaround for an apparent flaw with some
	 * AMD drivers that don't receive or interpret the length correctly, resulting in
	 * an access violation when the driver tries to read past the string memory.
	 *
	 * <p>Hat tip to fewizz for the find and the fix.
	 */
	private static void safeShaderSource(@NativeType("GLuint") int glId, @NativeType("GLchar const **") CharSequence source) {
		final MemoryStack stack = stackGet();
		final int stackPointer = stack.getPointer();

		try {
			final ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);
			final PointerBuffer pointers = stack.mallocPointer(1);
			pointers.put(sourceBuffer);

			GL21.nglShaderSource(glId, 1, pointers.address0(), 0);
			org.lwjgl.system.APIUtil.apiArrayFree(pointers.address0(), 1);
		} finally {
			stack.setPointer(stackPointer);
		}
	}

	protected String debugSourceString() {
		return shaderSourceId.getPath().toString().replace("/", "-").replace(":", "-");
	}

	private void outputDebugSource(String source, String error) {
		final String fileName = debugSourceString();
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

			if (Pipeline.shadowsEnabled()) {
				result = StringUtils.replace(result, "#define SHADOW_MAP_SIZE 1024", "#define SHADOW_MAP_SIZE " + Pipeline.skyShadowSize);
			} else {
				result = StringUtils.replace(result, "#define SHADOW_MAP_PRESENT", "//#define SHADOW_MAP_PRESENT");
				result = StringUtils.replace(result, "#define SHADOW_MAP_SIZE 1024", "//#define SHADOW_MAP_SIZE 1024");
			}

			result = StringUtils.replace(result, "#define _CV_MAX_SHADER_COUNT 0", "#define _CV_MAX_SHADER_COUNT " + MaterialShaderImpl.MAX_SHADERS);

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

	private String getCombinedShaderSource() {
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

		try (Resource resource = resourceManager.getResource(shaderSourceId)) {
			try (Reader reader = new InputStreamReader(resource.getInputStream())) {
				result = CharStreams.toString(reader);
			}
		} catch (final FileNotFoundException e) {
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
				if (tok == null) break;
				if (tok.getType() == Token.EOF) break;
				builder.append(tok.getText());
			}
		} catch (final Exception e) {
			CanvasMod.LOG.error("GLSL source pre-processing failed", e);
		}

		builder.append("\n");

		source = builder.toString();

		// restore GLSL version
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
