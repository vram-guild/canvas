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
import grondag.canvas.render.PrimaryFrameBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.config.FabulousConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.pipeline.config.SkyShadowConfig;
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
	public static int skyShadowSize;
	public static int skyShadowDepth;
	public static float shadowSlopeFactor = SkyShadowConfig.DEFAULT_SHADOW_SLOPE_FACTOR;
	public static float shadowBiasUnits = SkyShadowConfig.DEFAULT_SHADOW_BIAS_UNITS;

	public static float defaultZenithAngle = 0f;

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

	static void activate(PrimaryFrameBuffer primary, int width, int height) {
		assert RenderSystem.isOnRenderThread();

		if (reload || lastWidth != width || lastHeight != height) {
			reload = false;
			lastWidth = width;
			lastHeight = height;
			closeInner();
			activateInner(primary, width, height);
		}
	}

	private static void activateInner(PrimaryFrameBuffer primary, int width, int height) {
		final PipelineConfig config = PipelineConfigBuilder.build(new Identifier(Configurator.pipelineId));
		Pipeline.config = config;

		for (final ImageConfig img : config.images) {
			if (IMAGES.containsKey(img.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline image definition encountered with name %s. Duplicate was skipped.", img.name));
				continue;
			}

			IMAGES.put(img.name, new Image(img, img.width > 0 ? img.width : width, img.height > 0 ? img.height : height));
		}

		for (final ProgramConfig program : config.programs) {
			if (SHADERS.containsKey(program.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline shader definition encountered with name %s. Duplicate was skipped.", program.name));
				continue;
			}

			SHADERS.put(program.name, new ProcessShader(program.vertexSource, program.fragmentSource, program.samplerNames));
		}

		for (final FramebufferConfig buffer : config.framebuffers) {
			if (FRAMEBUFFERS.containsKey(buffer.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline framebuffer definition encountered with name %s. Duplicate was skipped.", buffer.name));
				continue;
			}

			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer, width, height));
		}

		PipelineFramebuffer b = getFramebuffer(config.defaultFramebuffer.name);
		defaultFbo = b;
		defaultColor = getImage(b.config.colorAttachments[0].image.name).glId();
		defaultDepth = getImage(b.config.depthAttachment.image.name).glId();

		primary.fbo = defaultFbo.glId();
		primary.colorAttachment = defaultColor;
		primary.depthAttachment = defaultDepth;

		solidTerrainFbo = getFramebuffer(config.drawTargets.solidTerrain.name);
		translucentTerrainFbo = getFramebuffer(config.drawTargets.translucentTerrain.name);
		translucentEntityFbo = getFramebuffer(config.drawTargets.translucentEntity.name);
		weatherFbo = getFramebuffer(config.drawTargets.weather.name);
		cloudsFbo = getFramebuffer(config.drawTargets.clouds.name);
		translucentParticlesFbo = getFramebuffer(config.drawTargets.translucentParticles.name);

		if (config.skyShadow != null) {
			skyShadowFbo = getFramebuffer(config.skyShadow.framebuffer.name);
			final Image sd = getImage(config.skyShadow.framebuffer.value().depthAttachment.image.name);
			shadowMapDepth = sd.glId();
			skyShadowSize = sd.config.width;
			shadowSlopeFactor = config.skyShadow.offsetSlopeFactor;
			shadowBiasUnits = config.skyShadow.offsetBiasUnits;
		} else {
			skyShadowFbo = null;
			shadowMapDepth = -1;
			skyShadowSize = 0;
			shadowSlopeFactor = SkyShadowConfig.DEFAULT_SHADOW_SLOPE_FACTOR;
			shadowBiasUnits = SkyShadowConfig.DEFAULT_SHADOW_BIAS_UNITS;
		}

		if (config.sky != null) {
			defaultZenithAngle = config.sky.defaultZenithAngle;
		} else {
			defaultZenithAngle = 0f;
		}

		isFabulous = config.fabulosity != null;

		if (isFabulous) {
			final FabulousConfig fc = config.fabulosity;
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

			fabulous = new Pass[config.fabulous.length];

			for (int i = 0; i < config.fabulous.length; ++i) {
				fabulous[i] = Pass.create(config.fabulous[i]);
			}
		}

		BufferDebug.init(config);

		onWorldRenderStart = buildPasses(config, config.onWorldStart);
		afterRenderHand = buildPasses(config, config.afterRenderHand);
	}

	private static Pass[] buildPasses(PipelineConfig cfg, PassConfig[] configs) {
		final ObjectArrayList<Pass> passes = new ObjectArrayList<>();

		for (int i = 0; i < configs.length; ++i) {
			passes.add(Pass.create(configs[i]));
		}

		return passes.toArray(new Pass[passes.size()]);
	}

	public static boolean isFabulous() {
		return isFabulous;
	}
}
