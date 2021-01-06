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
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.config.FabulousConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.pipeline.pass.Pass;
import grondag.canvas.shader.ProcessShader;

public class Pipeline {
	private static boolean reload = true;
	private static int lastWidth;
	private static int lastHeight;
	static Pass[] onWorldRenderStart = { };
	static Pass[] afterRenderHand = { };
	static Pass[] fabulous = { };

	private static boolean isFabulous = false;

	public static PipelineFramebuffer defaultFbo;
	public static int defaultColor = -1;
	public static int defaultDepth = -1;
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
	public static int shadowMapDepth = -1;

	public static PipelineFramebuffer skyShadowFbo;
	public static PipelineFramebuffer solidTerrainFbo;
	public static PipelineFramebuffer translucentTerrainFbo;
	public static PipelineFramebuffer translucentEntityFbo;
	public static PipelineFramebuffer weatherFbo;
	public static PipelineFramebuffer cloudsFbo;
	public static PipelineFramebuffer translucentParticlesFbo;

	private static final Object2ObjectOpenHashMap<String, Image> IMAGES = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, ProcessShader> SHADERS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, PipelineFramebuffer> FRAMEBUFFERS = new Object2ObjectOpenHashMap<>();

	private static PipelineConfig config;

	public static PipelineConfig config() {
		return config;
	}

	public static Image getImage(String name) {
		return IMAGES.get(name);
	}

	public static ProcessShader getShader(String name) {
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
		final PipelineConfig cfg = PipelineConfigBuilder.build(new Identifier(Configurator.pipelineId));
		config = cfg;

		for (final ImageConfig img : cfg.images) {
			if (IMAGES.containsKey(img.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline image definition encountered with name %s. Duplicate was skipped.", img.name));
				continue;
			}

			IMAGES.put(img.name, new Image(img, width, height));
		}

		for (final ProgramConfig shader : cfg.shaders) {
			if (SHADERS.containsKey(shader.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline shader definition encountered with name %s. Duplicate was skipped.", shader.name));
				continue;
			}

			SHADERS.put(shader.name, new ProcessShader(shader.vertexSource, shader.fragmentSource, shader.samplerNames));
		}

		for (final FramebufferConfig buffer : cfg.framebuffers) {
			if (FRAMEBUFFERS.containsKey(buffer.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline framebuffer definition encountered with name %s. Duplicate was skipped.", buffer.name));
				continue;
			}

			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer, width, height));
		}

		PipelineFramebuffer b = getFramebuffer(cfg.defaultFramebuffer.name);
		defaultFbo = b;
		defaultColor = getImage(b.config.colorAttachments[0].image.name).glId();
		defaultDepth = getImage(b.config.depthAttachment.image.name).glId();

		solidTerrainFbo = getFramebuffer(cfg.drawTargets.solidTerrain.name);
		translucentTerrainFbo = getFramebuffer(cfg.drawTargets.translucentTerrain.name);
		translucentEntityFbo = getFramebuffer(cfg.drawTargets.translucentEntity.name);
		weatherFbo = getFramebuffer(cfg.drawTargets.weather.name);
		cloudsFbo = getFramebuffer(cfg.drawTargets.clouds.name);
		translucentParticlesFbo = getFramebuffer(cfg.drawTargets.translucentParticles.name);

		if (cfg.skyShadow != null) {
			skyShadowFbo = getFramebuffer(cfg.skyShadow.framebuffer.name);
			shadowMapDepth = getImage(cfg.skyShadow.framebuffer.value().depthAttachment.image.name).glId();
		} else {
			skyShadowFbo = null;
			shadowMapDepth = -1;
		}

		isFabulous = cfg.fabulosity != null;

		if (isFabulous) {
			final FabulousConfig fc = cfg.fabulosity;
			b = getFramebuffer(fc.entityFrambuffer.name);
			fabEntityFbo = b.glId();
			fabEntityColor = getImage(b.config.colorAttachments[0].image.name).glId();
			fabEntityDepth = getImage(b.config.depthAttachment.image.name).glId();

			b = getFramebuffer(fc.particleFrambuffer.name);
			fabParticleFbo = b.glId();
			fabParticleColor = getImage(b.config.colorAttachments[0].image.name).glId();
			fabParticleDepth = getImage(b.config.depthAttachment.image.name).glId();

			b = getFramebuffer(fc.weatherFrambuffer.name);
			fabWeatherFbo = b.glId();
			fabWeatherColor = getImage(b.config.colorAttachments[0].image.name).glId();
			fabWeatherDepth = getImage(b.config.depthAttachment.image.name).glId();

			b = getFramebuffer(fc.cloudsFrambuffer.name);
			fabCloudsFbo = b.glId();
			fabCloudsColor = getImage(b.config.colorAttachments[0].image.name).glId();
			fabCloudsDepth = getImage(b.config.depthAttachment.image.name).glId();

			b = getFramebuffer(fc.translucentFrambuffer.name);
			fabTranslucentFbo = b.glId();
			fabTranslucentColor = getImage(b.config.colorAttachments[0].image.name).glId();
			fabTranslucentDepth = getImage(b.config.depthAttachment.image.name).glId();

			fabulous = new Pass[cfg.fabulous.length];

			for (int i = 0; i < cfg.fabulous.length; ++i) {
				fabulous[i] = Pass.create(cfg.fabulous[i]);
			}
		}

		BufferDebug.init(cfg);

		onWorldRenderStart = new Pass[cfg.onWorldStart.length];

		for (int i = 0; i < cfg.onWorldStart.length; ++i) {
			onWorldRenderStart[i] = Pass.create(cfg.onWorldStart[i]);
		}

		afterRenderHand = new Pass[cfg.afterRenderHand.length];

		for (int i = 0; i < cfg.afterRenderHand.length; ++i) {
			afterRenderHand[i] = Pass.create(cfg.afterRenderHand[i]);
		}
	}

	public static boolean isFabulous() {
		return isFabulous;
	}
}
