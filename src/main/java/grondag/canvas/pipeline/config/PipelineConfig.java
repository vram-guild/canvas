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

package grondag.canvas.pipeline.config;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.config.ConfigManager;
import grondag.canvas.pipeline.config.option.OptionConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class PipelineConfig {
	public final boolean smoothBrightnessBidirectionaly;
	public final int brightnessSmoothingFrames;
	public final int rainSmoothingFrames;
	public final boolean runVanillaClear;
	public final int glslVersion;
	public final boolean enablePBR;

	public final ConfigContext context;
	public final ImageConfig[] images;
	public final ProgramConfig[] programs;
	public final FramebufferConfig[] framebuffers;
	public final OptionConfig[] options;

	public final PassConfig[] onWorldStart;
	public final PassConfig[] afterRenderHand;
	public final PassConfig[] fabulous;

	@Nullable public final FabulousConfig fabulosity;
	@Nullable public final DrawTargetsConfig drawTargets;
	@Nullable public final SkyShadowConfig skyShadow;
	@Nullable public final SkyConfig sky;

	public final NamedDependency<FramebufferConfig> defaultFramebuffer;

	public final MaterialProgramConfig materialProgram;

	private final Object2ObjectOpenHashMap<ResourceLocation, OptionConfig> optionMap = new Object2ObjectOpenHashMap<>();

	private PipelineConfig() {
		smoothBrightnessBidirectionaly = false;
		brightnessSmoothingFrames = 20;
		rainSmoothingFrames = 500;
		runVanillaClear = true;
		glslVersion = 330;
		enablePBR = false;

		context = new ConfigContext();
		programs = new ProgramConfig[0];
		onWorldStart = new PassConfig[0];
		afterRenderHand = new PassConfig[0];
		fabulous = new PassConfig[0];
		images = new ImageConfig[] { ImageConfig.defaultMain(context), ImageConfig.defaultDepth(context) };
		framebuffers = new FramebufferConfig[] { FramebufferConfig.makeDefault(context) };
		options = new OptionConfig[0];
		fabulosity = null;
		skyShadow = null;
		sky = null;
		drawTargets = DrawTargetsConfig.makeDefault(context);
		defaultFramebuffer = context.frameBuffers.dependOn("default");
		materialProgram = new MaterialProgramConfig(context);
	}

	PipelineConfig (PipelineConfigBuilder builder) {
		context = builder.context;

		smoothBrightnessBidirectionaly = builder.smoothBrightnessBidirectionaly;
		brightnessSmoothingFrames = builder.brightnessSmoothingFrames;
		rainSmoothingFrames = builder.rainSmoothingFrames;
		runVanillaClear = builder.runVanillaClear;
		glslVersion = builder.glslVersion;
		enablePBR = builder.enablePBR;

		materialProgram = builder.materialProgram;
		defaultFramebuffer = builder.defaultFramebuffer;
		fabulosity = builder.fabulosity;
		drawTargets = builder.drawTargets;
		skyShadow = builder.skyShadow;
		sky = builder.sky;

		for (final OptionConfig opt : builder.options) {
			optionMap.put(opt.includeToken, opt);
		}

		options = builder.options.toArray(new OptionConfig[builder.options.size()]);

		fabulous = builder.fabulous.toArray(new PassConfig[builder.fabulous.size()]);
		images = builder.images.toArray(new ImageConfig[builder.images.size()]);
		programs = builder.programs.toArray(new ProgramConfig[builder.programs.size()]);
		framebuffers = builder.framebuffers.toArray(new FramebufferConfig[builder.framebuffers.size()]);
		onWorldStart = builder.onWorldStart.toArray(new PassConfig[builder.onWorldStart.size()]);
		afterRenderHand = builder.afterRenderHand.toArray(new PassConfig[builder.afterRenderHand.size()]);

		ConfigManager.initPipelineOptions(options);
	}

	public String configSource(ResourceLocation id) {
		final OptionConfig opt = optionMap.get(id);
		return opt == null ? null : opt.createSource();
	}

	public static PipelineConfig minimalConfig() {
		return new PipelineConfig();
	}

	public static final ResourceLocation DEFAULT_ID = new ResourceLocation("canvas:pipelines/canvas_standard.json5");
}
