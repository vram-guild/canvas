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

package grondag.canvas.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;

import grondag.canvas.mixinterface.FrameBufferExt;
import grondag.canvas.pipeline.PipelineConfig.DebugConfig;
import grondag.canvas.pipeline.PipelineConfig.ImageConfig;
import grondag.canvas.pipeline.PipelineConfig.ShaderConfig;
import grondag.canvas.shader.ProcessShader;

public class Pipeline {
	private static boolean reload = true;
	private static int lastWidth;
	private static int lastHeight;
	static Pass[] passes = { };

	private static final Object2ObjectOpenHashMap<Identifier, Image> IMAGES = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, ProcessShader> SHADERS = new Object2ObjectOpenHashMap<>();

	static Image getImage(Identifier id) {
		return IMAGES.get(id);
	}

	static ProcessShader getShader(Identifier id) {
		return SHADERS.get(id);
	}

	// WIP: remove
	static boolean needsReload() {
		return reload;
	}

	public static void reload() {
		reload = true;
	}

	public static void close() {
		closeInner();
		reload = true;
	}

	private static void closeInner() {
		for (final Pass pass : passes) {
			pass.close();
		}

		passes = new Pass[0];

		if (!IMAGES.isEmpty()) {
			IMAGES.values().forEach(img -> img.close());
			IMAGES.clear();
		}

		if (!SHADERS.isEmpty()) {
			SHADERS.values().forEach(shader -> shader.unload());
			SHADERS.clear();
		}

		BufferDebug.clear();
	}

	static void activate(int width, int height) {
		assert RenderSystem.isOnRenderThread();

		if (reload || lastWidth != width || lastHeight != height) {
			reload = false;
			lastWidth = width;
			lastHeight = height;
			closeInner();
			activateInner(width, height);
		}
	}

	private static void activateInner(int width, int height) {
		final PipelineConfig config = new PipelineConfig();

		final Framebuffer mcFbo = MinecraftClient.getInstance().getFramebuffer();
		final FrameBufferExt mcFboExt = ((FrameBufferExt) mcFbo);
		final int mainColor = mcFboExt.canvas_colorAttachment();

		IMAGES.put(PipelineConfig.IMG_MC_MAIN, new Image.BuiltIn(ImageConfig.of(PipelineConfig.IMG_MC_MAIN, false, false, 0), width, height, mainColor));

		for (final ImageConfig img : config.images) {
			IMAGES.put(img.id, new Image(img, width, height));
		}

		for (final ShaderConfig shader : config.shaders) {
			SHADERS.put(shader.id, new ProcessShader(shader.vertexSource, shader.fragmentSource, shader.samplerNames));
		}

		for (final DebugConfig debug : config.debugs) {
			BufferDebug.add(debug.mainImage, debug.sneakImage, debug.lod, debug.label);
		}

		final int passCount = config.afterRenderHand.passes.length;

		passes = new Pass[passCount];

		for (int i = 0; i < passCount; ++i) {
			passes[i] = new Pass(config.afterRenderHand.passes[i]);
			passes[i].open(width, height);
		}
	}
}
