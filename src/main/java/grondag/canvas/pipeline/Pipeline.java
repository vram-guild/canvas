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

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.pipeline.config.FabulousConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.shader.ProcessShader;

public class Pipeline {
	private static boolean reload = true;
	private static int lastWidth;
	private static int lastHeight;
	static Pass[] onWorldRenderStart = { };
	static Pass[] afterRenderHand = { };
	static Pass[] fabulous = { };

	private static boolean isFabulous = false;

	public static int fabEntityFbo = -1;
	public static int fabEntityColor = -1;
	public static int fabEntityDepth = -1;
	public static int fabParticleFbo = -1;
	public static int fabParticleColor = -1;
	public static int fabParticleDepth = -1;
	public static int fabWeatherFbo = -1;
	public static int fabWeatherColor = -1;
	public static int fabWeatherDepth = -1;
	public static int fabCloudsFbo = -1;
	public static int fabCloudsColor = -1;
	public static int fabCloudsDepth = -1;
	public static int fabTranslucentFbo = -1;
	public static int fabTranslucentColor = -1;
	public static int fabTranslucentDepth = -1;

	private static final Object2ObjectOpenHashMap<String, Image> IMAGES = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, ProcessShader> SHADERS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, PipelineFramebuffer> FRAMEBUFFERS = new Object2ObjectOpenHashMap<>();

	public static Image getImage(String name) {
		return IMAGES.get(name);
	}

	static ProcessShader getShader(String name) {
		return SHADERS.get(name);
	}

	public static PipelineFramebuffer getFramebuffer(String name) {
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

		for (final Pass pass : fabulous) {
			pass.close();
		}

		afterRenderHand = new Pass[0];
		onWorldRenderStart = new Pass[0];
		fabulous = new Pass[0];

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
		final PipelineConfig config = PipelineConfig.load(new Identifier(Configurator.pipelineId));

		for (final ImageConfig img : config.images) {
			if (IMAGES.containsKey(img.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline image definition encountered with name %s. Duplicate was skipped.", img.name));
				continue;
			}

			IMAGES.put(img.name, new Image(img, width, height));
		}

		for (final ProgramConfig shader : config.shaders) {
			if (SHADERS.containsKey(shader.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline shader definition encountered with name %s. Duplicate was skipped.", shader.name));
				continue;
			}

			SHADERS.put(shader.name, new ProcessShader(shader.vertexSource, shader.fragmentSource, shader.samplerNames));
		}

		for (final FramebufferConfig buffer : config.framebuffers) {
			if (FRAMEBUFFERS.containsKey(buffer.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline framebuffer definition encountered with name %s. Duplicate was skipped.", buffer.name));
				continue;
			}

			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer, width, height));
		}

		isFabulous = config.fabulosity != null;

		if (isFabulous) {
			final FabulousConfig fc = config.fabulosity;
			PipelineFramebuffer b = getFramebuffer(fc.entityFrambuffer);
			fabEntityFbo = b.glId();
			fabEntityColor = getImage(b.config.colorAttachments[0].imageName).glId();
			fabEntityDepth = getImage(b.config.depthAttachment.imageName).glId();

			b = getFramebuffer(fc.particleFrambuffer);
			fabParticleFbo = b.glId();
			fabParticleColor = getImage(b.config.colorAttachments[0].imageName).glId();
			fabParticleDepth = getImage(b.config.depthAttachment.imageName).glId();

			b = getFramebuffer(fc.weatherFrambuffer);
			fabWeatherFbo = b.glId();
			fabWeatherColor = getImage(b.config.colorAttachments[0].imageName).glId();
			fabWeatherDepth = getImage(b.config.depthAttachment.imageName).glId();

			b = getFramebuffer(fc.cloudsFrambuffer);
			fabCloudsFbo = b.glId();
			fabCloudsColor = getImage(b.config.colorAttachments[0].imageName).glId();
			fabCloudsDepth = getImage(b.config.depthAttachment.imageName).glId();

			b = getFramebuffer(fc.translucentFrambuffer);
			fabTranslucentFbo = b.glId();
			fabTranslucentColor = getImage(b.config.colorAttachments[0].imageName).glId();
			fabTranslucentDepth = getImage(b.config.depthAttachment.imageName).glId();

			fabulous = new Pass[config.fabulous.length];

			for (int i = 0; i < config.fabulous.length; ++i) {
				fabulous[i] = Pass.create(config.fabulous[i]);
			}
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

	public static boolean isFabulous() {
		return isFabulous;
	}
}
