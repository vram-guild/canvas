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
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import grondag.canvas.mixinterface.FrameBufferExt;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.ShaderConfig;
import grondag.canvas.shader.ProcessShader;

public class Pipeline {
	private static boolean reload = true;
	private static int lastWidth;
	private static int lastHeight;
	static Pass[] onWorldRenderStart = { };
	static Pass[] afterRenderHand = { };

	private static final Object2ObjectOpenHashMap<String, Image> IMAGES = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, ProcessShader> SHADERS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, PipelineFramebuffer> FRAMEBUFFERS = new Object2ObjectOpenHashMap<>();

	static Image getImage(String name) {
		return IMAGES.get(name);
	}

	static ProcessShader getShader(String name) {
		return SHADERS.get(name);
	}

	static PipelineFramebuffer getFramebuffer(String name) {
		return FRAMEBUFFERS.get(name);
	}

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
		for (final Pass pass : afterRenderHand) {
			pass.close();
		}

		for (final Pass pass : onWorldRenderStart) {
			pass.close();
		}

		afterRenderHand = new Pass[0];
		onWorldRenderStart = new Pass[0];

		if (!FRAMEBUFFERS.isEmpty()) {
			FRAMEBUFFERS.values().forEach(shader -> shader.close());
			FRAMEBUFFERS.clear();
		}

		if (!IMAGES.isEmpty()) {
			IMAGES.values().forEach(img -> img.close());
			IMAGES.clear();
		}

		if (!SHADERS.isEmpty()) {
			SHADERS.values().forEach(shader -> shader.unload());
			SHADERS.clear();
		}
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

		IMAGES.put("default_main", new Image.BuiltIn(ImageConfig.of("default_main", false, GL21.GL_RGBA8, GL21.GL_NEAREST, GL21.GL_NEAREST, 0), width, height, mainColor));

		for (final ImageConfig img : config.images) {
			IMAGES.put(img.name, new Image(img, width, height));
		}

		for (final ShaderConfig shader : config.shaders) {
			SHADERS.put(shader.name, new ProcessShader(shader.vertexSource, shader.fragmentSource, shader.samplerNames));
		}

		// WIP: add the mc framebuffers?

		for (final FramebufferConfig buffer : config.framebuffers) {
			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer, width, height));
		}

		BufferDebug.init(config);

		onWorldRenderStart = new Pass[config.onWorldStart.length];

		for (int i = 0; i < config.onWorldStart.length; ++i) {
			onWorldRenderStart[i] = Pass.create(config.onWorldStart[i]);
		}

		afterRenderHand = new Pass[config.afterRenderHand.length];

		for (int i = 0; i < config.afterRenderHand.length; ++i) {
			afterRenderHand[i] = Pass.create(config.afterRenderHand[i]);
		}
	}
}
