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
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineConfigBuilder;
import grondag.canvas.pipeline.config.ProgramConfig;
import grondag.canvas.pipeline.config.SkyShadowConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;
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

	public static PipelineFramebuffer defaultFbo;
	public static PipelineFramebuffer solidFbo;
	public static PipelineFramebuffer translucentTerrainFbo;
	public static PipelineFramebuffer translucentParticlesFbo;
	public static PipelineFramebuffer weatherFbo;
	public static PipelineFramebuffer cloudsFbo;
	public static PipelineFramebuffer translucentEntitiesFbo;
	public static PipelineFramebuffer skyShadowFbo;
	public static int shadowMapDepth = -1;
	public static int skyShadowSize;
	public static float shadowSlopeFactor = SkyShadowConfig.DEFAULT_SHADOW_SLOPE_FACTOR;
	public static float shadowBiasUnits = SkyShadowConfig.DEFAULT_SHADOW_BIAS_UNITS;

	public static float defaultZenithAngle = 0f;

	private static final Object2ObjectOpenHashMap<String, Image> IMAGES = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, ProcessProgram> PROGRAMS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<String, PipelineFramebuffer> FRAMEBUFFERS = new Object2ObjectOpenHashMap<>();

	private static PipelineConfig config;

	private static boolean advancedTerrainCulling;

	public static boolean shadowsEnabled() {
		return config.skyShadow != null;
	}

	public static boolean advancedTerrainCulling() {
		return advancedTerrainCulling;
	}

	public static PipelineConfig config() {
		return config;
	}

	public static Image getImage(ImageConfig image) {
		return IMAGES.get(image.name);
	}

	public static Image getImage(NamedDependency<ImageConfig> imageDep) {
		return getImage(imageDep.value());
	}

	public static ProcessProgram getProgram(String name) {
		return PROGRAMS.get(name);
	}

	public static PipelineFramebuffer getFramebuffer(FramebufferConfig fbConfig) {
		return FRAMEBUFFERS.get(fbConfig.name);
	}

	public static PipelineFramebuffer getFramebuffer(NamedDependency<FramebufferConfig> fbDep) {
		return getFramebuffer(fbDep.value());
	}

	public static ProgramTextureData materialTextures() {
		return materialTextures;
	}

	static PrimaryFrameBuffer getPrimaryFramebuffer() {
		Minecraft mc = Minecraft.getInstance();
		return (PrimaryFrameBuffer) mc.getMainRenderTarget();
	}

	public static int width() {
		return getPrimaryFramebuffer().width;
	}

	public static int height() {
		return getPrimaryFramebuffer().height;
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

		defaultFbo = null;
		solidFbo = null;
		translucentTerrainFbo = null;
		translucentParticlesFbo = null;
		weatherFbo = null;
		cloudsFbo = null;
		translucentEntitiesFbo = null;

		skyShadowFbo = null;

		shadowMapDepth = -1;
		skyShadowSize = 0;
		shadowSlopeFactor = SkyShadowConfig.DEFAULT_SHADOW_SLOPE_FACTOR;
		shadowBiasUnits = SkyShadowConfig.DEFAULT_SHADOW_BIAS_UNITS;
		advancedTerrainCulling = Configurator.advancedTerrainCulling;

		defaultZenithAngle = 0f;

		config = null;
	}

	static void activate() {
		final PipelineConfig config = PipelineConfigBuilder.build(new ResourceLocation(Configurator.pipelineId));
		Pipeline.config = config;

		Minecraft mc = Minecraft.getInstance();

		if (mc.getGpuWarnlistManager() != null) {
			mc.options.graphicsMode().set(isFabulous() ? GraphicsStatus.FABULOUS : GraphicsStatus.FANCY);
		}

		for (final ImageConfig img : config.images) {
			if (IMAGES.containsKey(img.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline image definition encountered with name %s. Duplicate was skipped.", img.name));
				continue;
			}

			IMAGES.put(img.name, new Image(img, img.width > 0 ? img.width : width(), img.height > 0 ? img.height : height()));
		}

		for (final ProgramConfig program : config.programs) {
			if (PROGRAMS.containsKey(program.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline shader definition encountered with name %s. Duplicate was skipped.", program.name));
				continue;
			}

			PROGRAMS.put(program.name, new ProcessProgram(program.name, program.vertexSource, program.fragmentSource, program.samplerNames));
		}

		initFramebuffers();

		if (shadowsEnabled()) {
			final Image sd = getFramebuffer(config.skyShadow.framebuffer).depthAttachment;
			shadowMapDepth = sd.glId();
			skyShadowSize = sd.config.width;
			shadowSlopeFactor = config.skyShadow.offsetSlopeFactor;
			shadowBiasUnits = config.skyShadow.offsetBiasUnits;
			advancedTerrainCulling = true;
		}

		if (config.sky != null) {
			defaultZenithAngle = config.sky.defaultZenithAngle;
		}

		if (isFabulous()) {
			fabulous = buildPasses(config.fabulous);
		}

		materialTextures = new ProgramTextureData(config.materialProgram.samplerImages);

		onWorldRenderStart = buildPasses(config.onWorldStart);
		afterRenderHand = buildPasses(config.afterRenderHand);
		onInit = buildPasses(config.onInit);
		onResize = buildPasses(config.onResize);

		BufferDebug.init(config);
	}

	static void onResize() {
		if (!FRAMEBUFFERS.isEmpty()) {
			FRAMEBUFFERS.values().forEach(PipelineFramebuffer::close);
			FRAMEBUFFERS.clear();
		}

		IMAGES.forEach((s, image) -> image.reallocateIfWindowSizeDependent(width(), height()));

		initFramebuffers();

		forEachPass(Pass::loadFramebuffer);
	}

	static void initFramebuffers() {
		for (final FramebufferConfig buffer : config.framebuffers) {
			if (FRAMEBUFFERS.containsKey(buffer.name)) {
				CanvasMod.LOG.warn(String.format("Duplicate pipeline framebuffer definition encountered with name %s. Duplicate was skipped.", buffer.name));
				continue;
			}

			FRAMEBUFFERS.put(buffer.name, new PipelineFramebuffer(buffer));
		}

		defaultFbo = getFramebuffer(config.defaultFramebuffer);
		solidFbo = getFramebuffer(config.drawTargets.solidTerrain);
		translucentTerrainFbo = getFramebuffer(config.drawTargets.translucentTerrain);
		translucentParticlesFbo = getFramebuffer(config.drawTargets.translucentParticles);
		weatherFbo = getFramebuffer(config.drawTargets.weather);
		cloudsFbo = getFramebuffer(config.drawTargets.clouds);
		translucentEntitiesFbo = getFramebuffer(config.drawTargets.translucentEntity);

		if (shadowsEnabled()) {
			skyShadowFbo = getFramebuffer(config.skyShadow.framebuffer);
		}

		Minecraft mc = Minecraft.getInstance();

		PrimaryFrameBuffer primary = (PrimaryFrameBuffer) mc.getMainRenderTarget();
		primary.frameBufferId = defaultFbo.glId();
		primary.colorTextureId = defaultFbo.colorAttachments[0].glId();
		primary.depthBufferId = defaultFbo.depthAttachment.glId();

		if (mc.levelRenderer != null) {
			((LevelRendererExt) mc.levelRenderer).canvas_setupFabulousBuffers();
		}
	}

	private static Pass[] buildPasses(PassConfig[] configs) {
		final ObjectArrayList<Pass> passes = new ObjectArrayList<>();

		for (PassConfig passConfig : configs) {
			passes.add(Pass.create(passConfig));
		}

		return passes.toArray(new Pass[0]);
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
		return config.fabulosity != null;
	}
}
