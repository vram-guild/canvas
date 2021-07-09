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
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.anarres.cpp.DefaultPreprocessorListener;
import org.anarres.cpp.Feature;
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.varia.GFX;
import grondag.frex.api.config.ShaderConfig;

public class GlShader implements Shader {
	static final Pattern PATTERN = Pattern.compile("^#include\\s+(\\\"*[\\w]+:[\\w/\\.]+)[ \\t]*.*", Pattern.MULTILINE);
	private static final HashSet<String> INCLUDED = new HashSet<>();
	private static boolean isErrorNoticeComplete = false;
	private static boolean needsClearDebugOutputWarning = true;
	private static boolean needsDebugOutputWarning = true;
	private final Identifier shaderSourceId;
	private final Supplier<String> sourceSupplier;
	protected final int shaderType;
	protected final ProgramType programType;
	private String source = null;
	private int glId = -1;
	private boolean needsLoad = true;
	private boolean isErrored = false;

	//WIP: wow this is ugly
	private String geometrySource;
	public GlShader vertexShader;

	public GlShader(Identifier shaderSourceId, int shaderType, ProgramType programType) {
		this.shaderSourceId = shaderSourceId;
		this.shaderType = shaderType;
		this.programType = programType;
		sourceSupplier = null;
	}

	public GlShader(Identifier shaderSourceId, Supplier<String> sourceSupplier, int shaderType, ProgramType programType) {
		this.shaderSourceId = shaderSourceId;
		this.shaderType = shaderType;
		this.programType = programType;
		this.sourceSupplier = sourceSupplier;
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
					CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_shader_output", path), e);
					needsDebugOutputWarning = false;
				}
			}
		}
	}

	// WIP: clean up
	private ObjectArrayList<String> varNames = null;

	private String getSource() {
		if (sourceSupplier != null) {
			return sourceSupplier.get();
		}

		String result = source;

		if (result == null) {
			result = getCombinedShaderSource();

			if (programType.isTerrain) {
				result = StringUtils.replace(result, "#define _CV_VERTEX_DEFAULT", "#define _CV_VERTEX_" + Configurator.terrainVertexConfig.name().toUpperCase());
				result = StringUtils.replace(result, "//#define _CV_IS_TERRAIN", "#define _CV_IS_TERRAIN");
			}

			if (programType.hasVertexProgramControl) {
				result = StringUtils.replace(result, "#define PROGRAM_BY_UNIFORM", "//#define PROGRAM_BY_UNIFORM");
			}

			if (shaderType == GL21.GL_FRAGMENT_SHADER) {
				result = StringUtils.replace(result, "#define VERTEX_SHADER", "#define FRAGMENT_SHADER");
			} else if (shaderType == GFX.GL_GEOMETRY_SHADER) {
				result = StringUtils.replace(result, "#define VERTEX_SHADER", "#define GEOMETRY_SHADER");
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

			//if (Configurator.hdLightmaps()) {
			//	result = StringUtils.replace(result, "#define VANILLA_LIGHTING", "//#define VANILLA_LIGHTING");
			//
			//	if (Configurator.lightmapNoise) {
			//		result = StringUtils.replace(result, "//#define ENABLE_LIGHT_NOISE", "#define ENABLE_LIGHT_NOISE");
			//	}
			//}

			result = glslPreprocessSource(result);

			if (programType.isTerrain && Configurator.geom) {
				if (shaderType == GFX.GL_VERTEX_SHADER) {
					varNames = new ObjectArrayList<>();
					result = glslPreprocessSource(result);
					//final String cleanSource = glslPreprocessSource(result);
					geometrySource = generateGeometrySource(result);
					result = updateVertexSourceForGeometry(result);
				} else {
					// Geometry shaders get generated source from vertex shader and should never get to here
					assert shaderType == GFX.GL_FRAGMENT_SHADER;

					if (vertexShader != null) {
						result = vertexShader.updateFragmentSourceForGeometry(result);
					}
				}
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
			final String result = Pipeline.config().configSource(shaderSourceId);
			return result == null ? ShaderConfig.getShaderConfigSupplier(shaderSourceId).get() : result;
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

	private static String glslPreprocessSource(String source) {
		source = StringUtils.replace(source, "#version ", "//#version ");

		@SuppressWarnings("resource")
		Preprocessor pp = new Preprocessor();
		pp.setListener(new DefaultPreprocessorListener());
		pp.addInput(new StringLexerSource(source, true));
		pp.addFeature(Feature.KEEPCOMMENTS);

		final StringBuilder builder = new StringBuilder();

		try {
			for (;;) {
				Token tok = pp.token();
				if (tok == null) break;
				if (tok.getType() == Token.EOF) break;
				builder.append(tok.getText());
			}
		} catch (Exception e) {
			CanvasMod.LOG.error("GLSL source pre-processing failed", e);
		}

		builder.append("\n");

		source = builder.toString();

		source = StringUtils.replace(source, "//#version ", "#version ");

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

	public String geometrySource() {
		return geometrySource;
	}

	private static final Pattern OUT_PATTERN = Pattern.compile("^\\s*((?:flat\\s)?out\\s+(?:(?:[iu]?vec[2-4])|(?:u?int)|(?:float))\\s+.+;)\s*$", Pattern.MULTILINE);

	private ObjectArrayList<String> extractOutVars(String source) {
		final ObjectArrayList<String> outVars = new ObjectArrayList<>();
		final Matcher m = OUT_PATTERN.matcher(source);

		while (m.find()) {
			System.out.println(m.group(1));
			outVars.add(m.group(1));
		}

		return outVars;
	}

	private String generateGeometrySource(String source) {
		varNames = extractOutVars(source);

		final StringBuilder builder = new StringBuilder();
		builder.append("#version 330\n\n");
		builder.append("/******************************************************\n");
		builder.append("/ CODE GENERATED AUTOMATICALLY BY CANVAS\n");
		builder.append("******************************************************/\n\n");

		builder.append("layout (lines_adjacency) in;\n");
		builder.append("layout (triangle_strip, max_vertices = 4) out;\n\n");

		//// build in interface block
		builder.append("in CanvasVars {\n");

		for (String varName : varNames) {
			builder.append("\t").append(geometryInput(varName)).append("\n");
		}

		builder.append("};\n\n");

		//// built out interface block
		builder.append("out CanvasVars {\n");

		for (String varName : varNames) {
			builder.append("\t").append(StringUtils.replace(varName, "out ", "")).append("\n");
		}

		builder.append("} outVars;\n\n");

		//// main func
		builder.append("void main() {\n");

		emitVertex(0, builder);
		emitVertex(1, builder);
		//emitVertex(2, varNames, builder);
		emitVertex(3, builder);
		builder.append("\tEndPrimitive();\n\n");

		emitVertex(2, builder);
		builder.append("\tEndPrimitive();\n");

		builder.append("}\n");

		return builder.toString();
	}

	private void emitVertex(int vertex, StringBuilder builder) {
		builder.append("\tgl_Position = gl_in[" + vertex + "].gl_Position;\n");
		final int limit = varNames.size();

		for (int i = 0; i < limit; ++i) {
			String varName = varNames.get(i).replaceAll(".* ", "").replace(";", "");
			builder
				.append("\toutVars.")
				.append(varName)
				.append(" = ")
				.append(varName)
				.append("[")
				.append(vertex)
				.append("];\n");
		}

		builder.append("\tEmitVertex();\n\n");
	}

	private static String geometryInput(String varName) {
		varName = varName.replaceAll("out ", "");
		return varName.replaceAll("((?:[iu]?vec[2-4])|(?:u?int)|(?:float))", "$1\\[\\]");
	}

	private String updateVertexSourceForGeometry(String source) {
		for (String varName : varNames) {
			source = StringUtils.replace(source, varName, "// " + varName + " // <- Moved to CanvasVars interface block //");
		}

		final StringBuilder builder = new StringBuilder();
		builder.append("\nout CanvasVars {\n");

		for (String varName : varNames) {
			builder.append("\t").append(StringUtils.replace(varName, "out ", "")).append("\n");
		}

		builder.append("};\n\n");

		source = StringUtils.replace(source, "#version 330\n", "#version 330\n" + builder.toString());

		return source;
	}

	public String updateFragmentSourceForGeometry(String source) {
		if (varNames == null) {
			return source;
		}

		for (String varName : varNames) {
			final String inName = StringUtils.replace(varName, "out ", "in ");
			source = StringUtils.replace(source, inName, "// " + inName + " // <- Moved to CanvasVars interface block //");
		}

		final StringBuilder builder = new StringBuilder();
		builder.append("\nin CanvasVars {\n");

		for (String varName : varNames) {
			builder.append("\t").append(StringUtils.replace(varName, "out ", "")).append("\n");
		}

		builder.append("};\n\n");

		source = StringUtils.replace(source, "#version 330\n", "#version 330\n" + builder.toString());

		return source;
	}
}
