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

package grondag.canvas.pipeline;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.pipeline.config.FabulousConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.pipeline.config.SkyShadowConfig;
import grondag.canvas.pipeline.pass.Pass;
import grondag.canvas.render.PrimaryFrameBuffer;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessProgram;

public class Pipeline {
	private static ProgramTextureData materialTextures;
	static Pass[] onWorldRenderStart = { };
	static Pass[] afterRenderHand = { };
	static Pass[] fabulous = { };
	static Pass[] onInit = { };
	static Pass[] onResize = { };

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
	private static final Object2ObjectOpenHashMap<String, ProcessProgram> PROGRAMS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, PipelineFramebuffer> FRAMEBUFFERS = new Object2ObjectOpenHashMap<>();

	private static PipelineConfig config;

	private static boolean advancedTerrainCulling;
	private static boolean coloredLightsEnabled;

	public static boolean shadowsEnabled() {
		return skyShadowFbo != null;
	}

	public static boolean coloredLightsEnabled() {
		return coloredLightsEnabled;
	}

	public static boolean advancedTerrainCulling() {
		return advancedTerrainCulling;
	}

	public static PipelineConfig config() {
		return config;
	}

	public static Image getImage(String name) {
		return IMAGES.get(name);
	}

	public static ProcessProgram getProgram(String name) {
		return PROGRAMS.get(name);
	}

	public static PipelineFramebuffer getFramebuffer(String name) {
		return FRAMEBUFFERS.get(name);
	}

	public static ProgramTextureData materialTextures() {
		return materialTextures;
	}

	public static void close() {
		forEachPass(Pass::close);

		afterRenderHand = new Pass[0];
		onWorldRenderStart = new Pass[0];
		fabulous = new Pass[0];
		onInit = new Pass[0];
		onResize = new Pass[0];

		if (!FRAMEBUFFERS.isEmpty()) {
			FRAMEBUFFERS.values().forEach(PipelineFramebuffer::close);
			FRAMEBUFFERS.clear();
		}

		if (!IMAGES.isEmpty()) {
			IMAGES.values().forEach(Image::close);
			IMAGES.clear();
		}

		if (!PROGRAMS.isEmpty()) {
			PROGRAMS.values().forEach(GlProgram::unload);
			PROGRAMS.clear();
		}
	}

	static void activate(PrimaryFrameBuffer primary, int width, int height) {
		final PipelineConfig config = PipelineConfigBuilder.build(new ResourceLocation(Configurator.pipelineId));
		Pipeline.config = config;

		isFabulous = config.fabulosity != null;

		Minecraft mc = Minecraft.getInstance();

		if (mc.getGpuWarnlistManager() != null) {
			mc.options.graphicsMode().set(isFabulous ? GraphicsStatus.FABULOUS : GraphicsStatus.FANCY);
		}

		for (final ImageConfig img : config.images) {
			if (IMAGES.containsKey(img.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline image definition encountered with name %s. Duplicate was skipped.", img.name));
				continue;
			}

			IMAGES.put(img.name, new Image(img, img.width > 0 ? img.width : width, img.height > 0 ? img.height : height));
		}

		for (final ProgramConfig program : config.programs) {
			if (PROGRAMS.containsKey(program.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline shader definition encountered with name %s. Duplicate was skipped.", program.name));
				continue;
			}

			PROGRAMS.put(program.name, new ProcessProgram(program.name, program.vertexSource, program.fragmentSource, program.samplerNames));
		}

		defaultColor = getImage(config.defaultFramebuffer.value().colorAttachments[0].image.name).glId();
		defaultDepth = getImage(config.defaultFramebuffer.value().depthAttachment.image.name).glId();

		initFramebuffers(primary);

		if (config.skyShadow != null) {
			final Image sd = getImage(config.skyShadow.framebuffer.value().depthAttachment.image.name);
			shadowMapDepth = sd.glId();
			skyShadowSize = sd.config.width;
			shadowSlopeFactor = config.skyShadow.offsetSlopeFactor;
			shadowBiasUnits = config.skyShadow.offsetBiasUnits;
			advancedTerrainCulling = true;
		} else {
			shadowMapDepth = -1;
			skyShadowSize = 0;
			shadowSlopeFactor = SkyShadowConfig.DEFAULT_SHADOW_SLOPE_FACTOR;
			shadowBiasUnits = SkyShadowConfig.DEFAULT_SHADOW_BIAS_UNITS;
			advancedTerrainCulling = Configurator.advancedTerrainCulling;
		}

		if (config.sky != null) {
			defaultZenithAngle = config.sky.defaultZenithAngle;
		} else {
			defaultZenithAngle = 0f;
		}

		coloredLightsEnabled = config.coloredLights != null;

		if (isFabulous) {
			final FabulousConfig fc = config.fabulosity;

			fabEntityColor = getImage(fc.entityFramebuffer.value().colorAttachments[0].image.name).glId();
			fabEntityDepth = getImage(fc.entityFramebuffer.value().depthAttachment.image.name).glId();

			fabParticleColor = getImage(fc.particleFramebuffer.value().colorAttachments[0].image.name).glId();
			fabParticleDepth = getImage(fc.entityFramebuffer.value().depthAttachment.image.name).glId();

			fabWeatherColor = getImage(fc.weatherFramebuffer.value().colorAttachments[0].image.name).glId();
			fabWeatherDepth = getImage(fc.weatherFramebuffer.value().depthAttachment.image.name).glId();

			fabCloudsColor = getImage(fc.cloudsFramebuffer.value().colorAttachments[0].image.name).glId();
			fabCloudsDepth = getImage(fc.cloudsFramebuffer.value().depthAttachment.image.name).glId();

			fabTranslucentColor = getImage(fc.translucentFramebuffer.value().colorAttachments[0].image.name).glId();
			fabTranslucentDepth = getImage(fc.translucentFramebuffer.value().depthAttachment.image.name).glId();

			fabulous = buildPasses(config, config.fabulous);
		} else {
			fabEntityColor = 0;
			fabEntityDepth = 0;

			fabParticleColor = 0;
			fabParticleDepth = 0;

			fabWeatherColor = 0;
			fabWeatherDepth = 0;

			fabCloudsColor = 0;
			fabCloudsDepth = 0;

			fabTranslucentColor = 0;
			fabTranslucentDepth = 0;
			fabulous = new Pass[0];
		}

		materialTextures = new ProgramTextureData(config.materialProgram.samplerImages);

		onWorldRenderStart = buildPasses(config, config.onWorldStart);
		afterRenderHand = buildPasses(config, config.afterRenderHand);
		onInit = buildPasses(config, config.onInit);
		onResize = buildPasses(config, config.onResize);

		BufferDebug.init(config);
	}

	static void onResize(PrimaryFrameBuffer primary, int width, int height) {
		if (!FRAMEBUFFERS.isEmpty()) {
			FRAMEBUFFERS.values().forEach(framebuffer -> framebuffer.close());
			FRAMEBUFFERS.clear();
		}

		IMAGES.forEach((s, image) -> image.reallocateIfWindowSizeDependent(width, height));

		initFramebuffers(primary);

		forEachPass(Pass::loadFramebuffer);
	}

	static void initFramebuffers(PrimaryFrameBuffer primary) {
		for (final FramebufferConfig buffer : config.framebuffers) {
			if (FRAMEBUFFERS.containsKey(buffer.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline framebuffer definition encountered with name %s. Duplicate was skipped.", buffer.name));
				continue;
			}

			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer));
		}

		defaultFbo = getFramebuffer(config.defaultFramebuffer.name);

		primary.frameBufferId = defaultFbo.glId();
		primary.colorTextureId = defaultColor;
		primary.depthBufferId = defaultDepth;

		solidTerrainFbo = getFramebuffer(config.drawTargets.solidTerrain.name);
		translucentTerrainFbo = getFramebuffer(config.drawTargets.translucentTerrain.name);
		translucentEntityFbo = getFramebuffer(config.drawTargets.translucentEntity.name);
		weatherFbo = getFramebuffer(config.drawTargets.weather.name);
		cloudsFbo = getFramebuffer(config.drawTargets.clouds.name);
		translucentParticlesFbo = getFramebuffer(config.drawTargets.translucentParticles.name);

		skyShadowFbo = config.skyShadow != null ? getFramebuffer(config.skyShadow.framebuffer.name) : null;

		if (isFabulous) {
			final FabulousConfig fc = config.fabulosity;

			fabEntityFbo = getFramebuffer(fc.entityFramebuffer.name).glId();
			fabParticleFbo = getFramebuffer(fc.particleFramebuffer.name).glId();
			fabWeatherFbo = getFramebuffer(fc.weatherFramebuffer.name).glId();
			fabCloudsFbo = getFramebuffer(fc.cloudsFramebuffer.name).glId();
			fabTranslucentFbo = getFramebuffer(fc.translucentFramebuffer.name).glId();
		} else {
			fabEntityFbo = 0;
			fabParticleFbo = 0;
			fabWeatherFbo = 0;
			fabCloudsFbo = 0;
			fabTranslucentFbo = 0;
		}

		Minecraft mc = Minecraft.getInstance();

		if (mc.levelRenderer != null) {
			((LevelRendererExt) mc.levelRenderer).canvas_setupFabulousBuffers();
		}
	}

	private static Pass[] buildPasses(PipelineConfig cfg, PassConfig[] configs) {
		final ObjectArrayList<Pass> passes = new ObjectArrayList<>();

		for (int i = 0; i < configs.length; ++i) {
			passes.add(Pass.create(configs[i]));
		}

		return passes.toArray(new Pass[passes.size()]);
	}

	private static void forEachPass(Consumer<Pass> consumer) {
		for (final Pass pass : afterRenderHand) {
			consumer.accept(pass);
		}

		for (final Pass pass : onWorldRenderStart) {
			consumer.accept(pass);
		}

		for (final Pass pass : fabulous) {
			consumer.accept(pass);
		}

		for (final Pass pass : onInit) {
			consumer.accept(pass);
		}

		for (final Pass pass : onResize) {
			consumer.accept(pass);
		}
	}

	public static boolean isFabulous() {
		return isFabulous;
	}
}
