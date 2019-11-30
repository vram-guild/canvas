/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.material;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.common.io.CharStreams;
import com.mojang.blaze3d.platform.GLX;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.Configurator.AoMode;
import grondag.canvas.Configurator.DiffuseMode;
import grondag.canvas.varia.CanvasGlHelper;

abstract class AbstractGlShader {
	private static boolean isErrorNoticeComplete = false;

	public final Identifier shaderSource;

	private final int shaderType;
	private final int shaderProps;
	private final ShaderContext context;

	private int glId = -1;
	private boolean needsLoad = true;
	private boolean isErrored = false;

	AbstractGlShader(Identifier shaderSource, int shaderType, int shaderProps, ShaderContext context) {
		this.shaderSource = shaderSource;
		this.shaderType = shaderType;
		this.shaderProps = shaderProps;
		this.context = context;
	}

	/**
	 * Call after render / resource refresh to force shader reload.
	 */
	public final void forceReload() {
		needsLoad = true;
	}

	public final int glId() {
		if (needsLoad) {
			load();
		}

		return isErrored ? -1 : glId;
	}

	private final void load() {
		needsLoad = false;
		isErrored = false;
		String source = null;
		String error = null;

		try {
			if (glId <= 0) {
				glId = GLX.glCreateShader(shaderType);
				if (glId == 0) {
					glId = -1;
					isErrored = true;
					return;
				}
			}

			source = getSource();

			GLX.glShaderSource(glId, source);
			GLX.glCompileShader(glId);

			if (GLX.glGetShaderi(glId, GLX.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				isErrored = true;
				error = CanvasGlHelper.getShaderInfoLog(glId);
				if(error.isEmpty()) {
					error = "Unknown OpenGL Error.";
				}
			}

		} catch (final Exception e) {
			isErrored = true;
			error = e.getMessage();
		}

		if(isErrored) {
			if(glId > 0) {
				GLX.glDeleteShader(glId);
				glId = -1;
			}

			if(Configurator.conciseErrors) {
				if(!isErrorNoticeComplete) {
					CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_any_shader"));
					isErrorNoticeComplete = true;
				}
			} else {
				CanvasMod.LOG.error(I18n.translate("error.canvas.fail_create_shader", shaderSource.toString(), Integer.toString(shaderProps), error));
			}
			outputDebugSource(source, error);

		} else if(Configurator.shaderDebug) {
			outputDebugSource(source, null);
		}
	}

	public static void forceReloadErrors() {
		isErrorNoticeComplete = false;
		clearDebugSource();
	}

	private static void clearDebugSource() {
		try {
			final File gameDir = FabricLoader.getInstance().getGameDirectory();
			File shaderDir = new File(gameDir.getAbsolutePath().replace(".", "canvas_shader_debug"));

			if(shaderDir.exists()) {
				final File files[] = shaderDir.listFiles();
				for(final File f : files) {
					if (f.toString().endsWith(".glsl")) {
						f.delete();
					}
				}
			}

			shaderDir = new File(gameDir.getAbsolutePath().replace(".", "canvas_shader_debug/failed"));
			if(shaderDir.exists()) {
				final File files[] = shaderDir.listFiles();
				for(final File f : files) {
					if (f.toString().endsWith(".glsl")) {
						f.delete();
					}
				}
			}
		} catch(final Exception e){
			// eat it
		}
	}

	private void outputDebugSource(String source, String error) {
		final String key = shaderSource.toString().replace("/", "-") + "."  + context.toString() +  "." + shaderProps;
		final File gameDir = FabricLoader.getInstance().getGameDirectory();
		File shaderDir = new File(gameDir.getAbsolutePath().replace(".", "canvas_shader_debug"));
		if(!shaderDir.exists()) {
			shaderDir.mkdir();
		}
		if(error != null) {
			shaderDir = new File(gameDir.getAbsolutePath().replace(".", "canvas_shader_debug/failed"));
			if(!shaderDir.exists()) {
				shaderDir.mkdir();
			}
			source = "///////// ERROR ////////\n" + error + "\n////////////////////////\n\n" + source;
		}

		if(shaderDir.exists()) {
			try(
					FileWriter writer = new FileWriter(shaderDir.getAbsolutePath() + File.separator + key + ".glsl", false);
					) {
				writer.write(source);
				writer.close();
			} catch (final IOException e) {
				// eat it
			}
		}
	}

	public String buildSource(String librarySource) {
		String result = getShaderSource(shaderSource);
		result = result.replaceAll("#version\\s+120", "");
		result = librarySource + result;

		final int spriteDepth = ShaderProps.spriteDepth(shaderProps);

		if(ShaderProps.cutout(shaderProps)) {
			result = result.replaceAll("#define CUTOUT FALSE", "#define CUTOUT TRUE");
		}

		if (spriteDepth > 1) {
			result = result.replaceAll("#define LAYER_COUNT 1", String.format("#define LAYER_COUNT %d", spriteDepth));
		}

		result = result.replaceAll("#define CONTEXT 0", "#define CONTEXT " + context.ordinal());

		if(!context.isBlock) {
			result = result.replaceAll("#define CONTEXT_IS_BLOCK TRUE", "#define CONTEXT_IS_BLOCK FALSE");
		}

		if(Configurator.hardcoreDarkness && context != ShaderContext.ITEM_GUI) {
			result = result.replaceAll("#define HARDCORE_DARKNESS FALSE", "#define HARDCORE_DARKNESS TRUE");
		}

		if(Configurator.subtleFog && context != ShaderContext.ITEM_GUI) {
			result = result.replaceAll("#define SUBTLE_FOG FALSE", "#define SUBTLE_FOG TRUE");
		}

		if(!context.isBlock) {
			result = result.replaceAll("#define CONTEXT_IS_BLOCK TRUE", "#define CONTEXT_IS_BLOCK FALSE");
		}

		if(!Configurator.hdLightmaps || ((shaderProps & ShaderProps.SMOOTH_LIGHTMAPS) == 0)) {
			result = result.replaceAll("#define ENABLE_SMOOTH_LIGHT TRUE", "#define ENABLE_SMOOTH_LIGHT FALSE");
		}

		if(!Configurator.lightmapNoise || !Configurator.hdLightmaps) {
			result = result.replaceAll("#define ENABLE_LIGHT_NOISE TRUE", "#define ENABLE_LIGHT_NOISE FALSE");
		}

		if(Configurator.aoShadingMode != AoMode.NORMAL) {
			result = result.replaceAll("#define AO_SHADING_MODE AO_MODE_NORMAL",
					"#define AO_SHADING_MODE AO_MODE_" + Configurator.aoShadingMode.name());
		}

		if(Configurator.diffuseShadingMode != DiffuseMode.NORMAL) {
			result = result.replaceAll("#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_NORMAL",
					"#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_" + Configurator.diffuseShadingMode.name());
		}

		if(CanvasGlHelper.useGpuShader4() ) {
			result = result.replaceAll("#define USE_FLAT_VARYING FALSE", "#define USE_FLAT_VARYING TRUE");
		} else {
			result = result.replaceAll("#extension GL_EXT_gpu_shader4 : enable", "");
		}

		if((shaderProps & ShaderProps.WHITE_0) != 0) {
			result = result.replaceAll("#define WHITE_0 FALSE", "#define WHITE_0 TRUE");
		}

		return result;
	}

	abstract String getSource();

	public static String getShaderSource(Identifier shaderSource) {
		final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

		try(Resource resource = resourceManager.getResource(shaderSource)) {
			try (Reader reader = new InputStreamReader(resource.getInputStream())) {
				return CharStreams.toString(reader);
			}
		} catch (final IOException e) {
			return "";
		}
	}
}
