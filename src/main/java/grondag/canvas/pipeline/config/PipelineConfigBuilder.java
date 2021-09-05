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

import java.io.IOException;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.ConfigManager;
import grondag.canvas.pipeline.config.option.OptionConfig;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.LoadHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class PipelineConfigBuilder {
	public final ConfigContext context = new ConfigContext();
	public final ObjectArrayList<ImageConfig> images = new ObjectArrayList<>();
	public final ObjectArrayList<ProgramConfig> programs = new ObjectArrayList<>();
	public final ObjectArrayList<FramebufferConfig> framebuffers = new ObjectArrayList<>();
	public final ObjectArrayList<OptionConfig> options = new ObjectArrayList<>();

	public final ObjectArrayList<PassConfig> onWorldStart = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> afterRenderHand = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> fabulous = new ObjectArrayList<>();

	@Nullable public FabulousConfig fabulosity;
	@Nullable public DrawTargetsConfig drawTargets;
	@Nullable public SkyShadowConfig skyShadow;
	@Nullable public SkyConfig sky;

	public boolean smoothBrightnessBidirectionaly = false;
	public int brightnessSmoothingFrames = 20;
	public int rainSmoothingFrames = 500;
	public boolean runVanillaClear = true;
	public int glslVersion = 330;
	public boolean enablePBR = false;

	public NamedDependency<FramebufferConfig> defaultFramebuffer;

	public MaterialProgramConfig materialProgram;

	public void load(JsonObject configJson) {
		smoothBrightnessBidirectionaly = configJson.getBoolean("smoothBrightnessBidirectionaly", smoothBrightnessBidirectionaly);
		runVanillaClear = configJson.getBoolean("runVanillaClear", runVanillaClear);
		brightnessSmoothingFrames = configJson.getInt("brightnessSmoothingFrames", brightnessSmoothingFrames);
		rainSmoothingFrames = configJson.getInt("rainSmoothingFrames", rainSmoothingFrames);
		glslVersion = configJson.getInt("glslVersion", glslVersion);
		enablePBR = configJson.getBoolean("enablePBR", enablePBR);

		if (configJson.containsKey("materialProgram")) {
			if (materialProgram == null) {
				materialProgram = LoadHelper.loadObject(context, configJson, "materialProgram", MaterialProgramConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'materialProgram' ignored.");
			}
		}

		if (configJson.containsKey("defaultFramebuffer")) {
			if (defaultFramebuffer == null) {
				defaultFramebuffer = context.frameBuffers.dependOn(configJson, "defaultFramebuffer");
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'defaultFramebuffer' ignored.");
			}
		}

		if (configJson.containsKey("fabulousTargets")) {
			if (fabulosity == null) {
				fabulosity = LoadHelper.loadObject(context, configJson, "fabulousTargets", FabulousConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'fabulousTargets' ignored.");
			}
		}

		if (configJson.containsKey("skyShadows")) {
			if (skyShadow == null) {
				skyShadow = LoadHelper.loadObject(context, configJson, "skyShadows", SkyShadowConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'skyShadows' ignored.");
			}
		}

		if (configJson.containsKey("sky")) {
			if (sky == null) {
				sky = LoadHelper.loadObject(context, configJson, "sky", SkyConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'sky' ignored.");
			}
		}

		if (configJson.containsKey("materialVertexShader")) {
			CanvasMod.LOG.warn("Invalid pipeline config - obsolete 'materialVertexShader' attribute found - use 'materialProgram' instead.");
		}

		if (configJson.containsKey("materialFragmentShader")) {
			CanvasMod.LOG.warn("Invalid pipeline config - obsolete 'materialFragmentShader' attribute found - use 'materialProgram' instead.");
		}

		if (configJson.containsKey("drawTargets")) {
			if (drawTargets == null) {
				drawTargets = LoadHelper.loadObject(context, configJson, "drawTargets", DrawTargetsConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'drawTargets' ignored.");
			}
		}

		LoadHelper.loadSubList(context, configJson, "fabulous", "passes", fabulous, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "beforeWorldRender", "passes", onWorldStart, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "afterRenderHand", "passes", afterRenderHand, PassConfig::new);

		LoadHelper.loadList(context, configJson, "images", images, ImageConfig::new);
		LoadHelper.loadList(context, configJson, "programs", programs, ProgramConfig::new);
		LoadHelper.loadList(context, configJson, "framebuffers", framebuffers, FramebufferConfig::new);
		LoadHelper.loadList(context, configJson, "options", options, OptionConfig::new);
	}

	public boolean validate() {
		boolean valid = true;

		valid &= AbstractConfig.assertAndWarn(drawTargets != null && drawTargets.validate(), "Invalid pipeline config - missing or invalid drawTargets config.");

		valid &= AbstractConfig.assertAndWarn(materialProgram != null && materialProgram.validate(), "Invalid pipeline config - missing or invalid materialProgram.");

		valid &= (fabulosity == null || fabulosity.validate());
		valid &= (skyShadow == null || skyShadow.validate());

		valid &= defaultFramebuffer != null && defaultFramebuffer.validate("Invalid pipeline config - missing or invalid defaultFramebuffer.");

		for (final FramebufferConfig fb : framebuffers) {
			valid &= fb.validate();
		}

		for (final ImageConfig img : images) {
			valid &= img.validate();
		}

		for (final ProgramConfig prog : programs) {
			valid &= prog.validate();
		}

		for (final OptionConfig opt : options) {
			valid &= opt.validate();
		}

		return valid;
	}

	private static @Nullable PipelineConfigBuilder load(Identifier id) {
		final ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

		if (!PipelineLoader.areResourcesAvailable() || rm == null) {
			return null;
		}

		final PipelineConfigBuilder result = new PipelineConfigBuilder();
		final ObjectOpenHashSet<Identifier> included = new ObjectOpenHashSet<>();
		final ObjectArrayFIFOQueue<Identifier> queue = new ObjectArrayFIFOQueue<>();

		queue.enqueue(id);
		included.add(id);

		while (!queue.isEmpty()) {
			Identifier target = queue.dequeue();

			// Allow flexibility on JSON vs JSON5 extensions
			if (!rm.containsResource(target)) {
				if (target.getPath().endsWith("json5")) {
					final var candidate = new Identifier(target.getNamespace(), target.getPath().substring(0, target.getPath().length() - 1));

					if (rm.containsResource(candidate)) {
						target = candidate;
					}
				} else if (target.getPath().endsWith("json")) {
					final var candidate = new Identifier(target.getNamespace(), target.getPath() + "5");

					if (rm.containsResource(candidate)) {
						target = candidate;
					}
				}
			}

			try (Resource res = rm.getResource(target)) {
				final JsonObject configJson = ConfigManager.JANKSON.load(res.getInputStream());
				result.load(configJson);
				getIncludes(configJson, included, queue);
			} catch (final IOException e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to IOException: %s", target.toString(), e.getLocalizedMessage()));
			} catch (final SyntaxError e) {
				CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to Syntax Error: %s", target.toString(), e.getLocalizedMessage()));
			}
		}

		if (result.validate()) {
			return result;
		} else {
			// fallback to minimal renderable pipeline if not valid
			return null;
		}
	}

	private static void getIncludes(JsonObject configJson, ObjectOpenHashSet<Identifier> included, ObjectArrayFIFOQueue<Identifier> queue) {
		if (configJson == null || !configJson.containsKey("include")) {
			return;
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(configJson, "include", "Pipeline config error: 'include' must be an array.");
		final int limit = array.size();

		for (int i = 0; i < limit; ++i) {
			final String idString = JanksonHelper.asString(array.get(i));

			if (idString != null && !idString.isEmpty()) {
				final Identifier id = new Identifier(idString);

				if (included.add(id)) {
					queue.enqueue(id);
				}
			}
		}
	}

	public static PipelineConfig build(Identifier identifier) {
		final PipelineConfigBuilder builder = load(identifier);
		return builder == null ? PipelineConfig.minimalConfig() : new PipelineConfig(builder);
	}
}
