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

package grondag.canvas.pipeline.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

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
	public OptionConfig[] prebuiltOptions;

	public final ObjectArrayList<PassConfig> onWorldStart = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> afterRenderHand = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> fabulous = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> onInit = new ObjectArrayList<>();
	public final ObjectArrayList<PassConfig> onResize = new ObjectArrayList<>();

	@Nullable public FabulousConfig fabulosity;
	@Nullable public DrawTargetsConfig drawTargets;
	@Nullable public SkyShadowConfig skyShadow;
	@Nullable public SkyConfig sky;
	@Nullable public ColoredLightsConfig coloredLights;

	public boolean smoothBrightnessBidirectionaly = false;
	public int brightnessSmoothingFrames = 20;
	public int rainSmoothingFrames = 500;
	public int thunderSmoothingFrames = 500;
	public boolean runVanillaClear = true;
	public int glslVersion = 330;
	public boolean enablePBR = false;

	public NamedDependency<FramebufferConfig> defaultFramebuffer;

	public MaterialProgramConfig materialProgram;

	/**
	 * Priority-pass loading. Loads options before anything else. This is necessary for the
	 * current design of {@link grondag.canvas.pipeline.config.util.DynamicLoader}.
	 *
	 * @param configJson the json file being read
	 */
	public void loadPriority(JsonObject configJson) {
		LoadHelper.loadList(context, configJson, "options", options, OptionConfig::new);
	}

	/**
	 * Initialize the options immediately after all of them are loaded.
	 */
	private void afterLoadPriority() {
		ConfigManager.initPipelineOptions(prebuiltOptions = options.toArray(new OptionConfig[options.size()]));
	}

	public void load(JsonObject configJson) {
		smoothBrightnessBidirectionaly = context.dynamic.getBoolean(configJson, "smoothBrightnessBidirectionaly", smoothBrightnessBidirectionaly);
		runVanillaClear = context.dynamic.getBoolean(configJson, "runVanillaClear", runVanillaClear);
		brightnessSmoothingFrames = context.dynamic.getInt(configJson, "brightnessSmoothingFrames", brightnessSmoothingFrames);
		rainSmoothingFrames = context.dynamic.getInt(configJson, "rainSmoothingFrames", rainSmoothingFrames);
		thunderSmoothingFrames = context.dynamic.getInt(configJson, "thunderSmoothingFrames", thunderSmoothingFrames);
		glslVersion = context.dynamic.getInt(configJson, "glslVersion", glslVersion);
		enablePBR = context.dynamic.getBoolean(configJson, "enablePBR", enablePBR);

		if (glslVersion < 330) {
			CanvasMod.LOG.warn("Invalid pipeline config - GLSL version " + glslVersion + " < 330 ignored.");
			glslVersion = 330;
		}

		if (configJson.containsKey("materialProgram")) {
			if (materialProgram == null) {
				materialProgram = LoadHelper.loadObject(context, configJson, "materialProgram", MaterialProgramConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'materialProgram' ignored.");
			}
		}

		if (configJson.containsKey("defaultFramebuffer")) {
			if (defaultFramebuffer == null) {
				defaultFramebuffer = context.frameBuffers.dependOn(context.dynamic.getString(configJson, "defaultFramebuffer"));
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

		if (configJson.containsKey("coloredLights")) {
			if (coloredLights == null) {
				coloredLights = LoadHelper.loadObject(context, configJson, "coloredLights", ColoredLightsConfig::new);
			} else {
				CanvasMod.LOG.warn("Invalid pipeline config - duplicate 'coloredLights' ignored.");
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
		LoadHelper.loadSubList(context, configJson, "onInit", "passes", onInit, PassConfig::new);
		LoadHelper.loadSubList(context, configJson, "onResize", "passes", onResize, PassConfig::new);

		LoadHelper.loadList(context, configJson, "images", images, ImageConfig::new);
		LoadHelper.loadList(context, configJson, "programs", programs, ProgramConfig::new);
		LoadHelper.loadList(context, configJson, "framebuffers", framebuffers, FramebufferConfig::new);
	}

	public boolean validate() {
		boolean valid = true;

		valid &= AbstractConfig.assertAndWarn(drawTargets != null && drawTargets.validate(), "Invalid pipeline config - missing or invalid drawTargets config.");

		valid &= AbstractConfig.assertAndWarn(materialProgram != null && materialProgram.validate(), "Invalid pipeline config - missing or invalid materialProgram.");

		valid &= (fabulosity == null || fabulosity.validate());
		valid &= (skyShadow == null || skyShadow.validate());
		valid &= (coloredLights == null || coloredLights.validate());

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

	private static @Nullable PipelineConfigBuilder load(ResourceLocation id) {
		final ResourceManager rm = Minecraft.getInstance().getResourceManager();

		if (!PipelineLoader.areResourcesAvailable() || rm == null) {
			return null;
		}

		final var result = new PipelineConfigBuilder();
		final var included = new ObjectOpenHashSet<ResourceLocation>();
		final var readQueue = new ObjectArrayFIFOQueue<ResourceLocation>();
		final var primaryLoadQueue = new ObjectArrayFIFOQueue<JsonObject>();
		final var secondLoadQueue = new ObjectArrayFIFOQueue<JsonObject>();

		loadResources(id, readQueue, primaryLoadQueue, included, rm);

		while (!primaryLoadQueue.isEmpty()) {
			final JsonObject target = primaryLoadQueue.dequeue();
			secondLoadQueue.enqueue(target);
			result.loadPriority(target);
		}

		result.afterLoadPriority();

		while (!secondLoadQueue.isEmpty()) {
			final JsonObject target = secondLoadQueue.dequeue();
			result.load(target);
		}

		if (result.validate()) {
			return result;
		} else {
			// fallback to minimal renderable pipeline if not valid
			return null;
		}
	}

	/**
	 * This function is also used in {@link PipelineDescription} to parse the entire pipeline.
	 * Notably, this and subsequently called functions shouldn't contain any building code.
	 */
	static void loadResources(ResourceLocation id, ObjectArrayFIFOQueue<ResourceLocation> queue, ObjectArrayFIFOQueue<JsonObject> loadQueue, ObjectOpenHashSet<ResourceLocation> included, ResourceManager rm) {
		queue.enqueue(id);
		included.add(id);

		while (!queue.isEmpty()) {
			final ResourceLocation target = queue.dequeue();
			readResource(target, queue, loadQueue, included, rm);
		}
	}

	private static void readResource(ResourceLocation target, ObjectArrayFIFOQueue<ResourceLocation> queue, ObjectArrayFIFOQueue<JsonObject> loadQueue, ObjectOpenHashSet<ResourceLocation> included, ResourceManager rm) {
		// Allow flexibility on JSON vs JSON5 extensions
		if (rm.getResource(target).isEmpty()) {
			if (target.getPath().endsWith("json5")) {
				final var candidate = new ResourceLocation(target.getNamespace(), target.getPath().substring(0, target.getPath().length() - 1));

				if (rm.getResource(candidate).isPresent()) {
					target = candidate;
				}
			} else if (target.getPath().endsWith("json")) {
				final var candidate = new ResourceLocation(target.getNamespace(), target.getPath() + "5");

				if (rm.getResource(candidate).isPresent()) {
					target = candidate;
				}
			}
		}

		try (InputStream inputStream = rm.getResource(target).get().open()) {
			final JsonObject configJson = ConfigManager.JANKSON.load(inputStream);
			loadQueue.enqueue(configJson);
			getIncludes(configJson, included, queue);
		} catch (final IOException | NoSuchElementException e) {
			CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to IOException: %s", target.toString(), e.getLocalizedMessage()));
		} catch (final SyntaxError e) {
			CanvasMod.LOG.warn(String.format("Unable to load pipeline config resource %s due to Syntax Error: %s", target.toString(), e.getLocalizedMessage()));
		}
	}

	private static void getIncludes(JsonObject configJson, ObjectOpenHashSet<ResourceLocation> included, ObjectArrayFIFOQueue<ResourceLocation> queue) {
		if (configJson == null || !configJson.containsKey("include")) {
			return;
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(configJson, "include", "Pipeline config error: 'include' must be an array.");
		final int limit = array != null ? array.size() : 0;

		for (int i = 0; i < limit; ++i) {
			final String idString = JanksonHelper.asString(array.get(i));

			if (idString != null && !idString.isEmpty()) {
				final ResourceLocation id = new ResourceLocation(idString);

				if (included.add(id)) {
					queue.enqueue(id);
				}
			}
		}
	}

	public static PipelineConfig build(ResourceLocation identifier) {
		PipelineConfigBuilder builder = load(identifier);

		if (builder == null && !PipelineConfig.DEFAULT_ID.equals(identifier)) {
			builder = load(PipelineConfig.DEFAULT_ID);
		}

		return builder == null ? PipelineConfig.minimalConfig() : new PipelineConfig(builder);
	}
}
