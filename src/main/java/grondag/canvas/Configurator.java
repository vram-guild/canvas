/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.apiimpl.Canvas;

@Environment(EnvType.CLIENT)
public class Configurator {

	@SuppressWarnings("hiding")
	static class ConfigData {
		@Comment("Makes terrain fog a little less foggy or turns it off.")
		FogMode fogMode = FogMode.VANILLA;

		@Comment("Fluid biome colors are blended at block corners to avoid patchy appearance. Slight peformance impact to chunk loading.")
		boolean blendFluidColors = true;

		@Comment("Glow effect around light sources. Work-in-Progress")
		public boolean enableBloom = false;

		@Comment("Intensity of glow effect around light sources. 0.0 to 0.25, default is 0.09.")
		public float bloomIntensity = 0.06f;

		@Comment("Size of bloom effect around light sources. 0.0 to 2.0, default is 0.5.")
		public float bloomScale = 0.5f;

		@Comment("Truly smoothh lighting. Some impact to memory use, chunk loading and frame rate.")
		boolean hdLightmaps = false;

		@Comment("Slight variation in light values - may prevent banding. Slight performance impact and not usually necessary.")
		boolean lightmapNoise = false;

		@Comment("Mimics directional light.")
		DiffuseMode diffuseShadingMode = DiffuseMode.NORMAL;

		@Comment("Makes light sources less cross-shaped. Chunk loading a little slower. Overall light levels remain similar.")
		boolean lightSmoothing = false;

		@Comment("Mimics light blocked by nearby objects.")
		AoMode aoShadingMode = AoMode.NORMAL;

		@Comment("Setting > 0 may give slightly better FPS at cost of potential flickering when lighting changes.")
		int maxLightmapDelayFrames = 0;

		@Comment("Extra lightmap capacity. Ensure enabled if you are getting `unable to create HD lightmap(s) - out of space' messages.")
		boolean moreLightmap = true;

		@Comment("Models with flat lighting have smoother lighting (but no ambient occlusion).")
		boolean semiFlatLighting = true;

		// TWEAKS
		@Comment("Draws multiple chunks with same view transformation. Much faster, but try without if you see visual defects.")
		boolean batchedChunkRender = true;

		@Comment("Adjusts quads on some vanilla models (like iron bars) to avoid z-fighting with neighbor blocks.")
		boolean preventDepthFighting = true;

		@Comment("Treats model geometry outside of block boundaries as on the block for lighting purposes. Helps prevent bad lighting outcomes.")
		boolean clampExteriorVertices = true;

		@Comment("Prevent Glowstone and other blocks that emit light from casting shade on nearby blocks.")
		boolean fixLuminousBlockShading = true;

		@Comment("Terrain setup done off the main render thread. Increases FPS when moving. May see occasional flashes of blank chunks")
		boolean terrainSetupOffThread = true;

		@Comment("Use Vertex Array Objects if available. VAOs generally improve performance when they are supported.")
		boolean enableVao = true;

		@Comment("Use more efficient entity culling. Improves framerate in most scenes.")
		boolean cullEntityRender = true;

		// DEBUG
		@Comment("Output runtime per-material shader source. For shader development debugging.")
		boolean shaderDebug = false;

		@Comment("Shows HD lightmap pixels for debug purposes. Also looks cool.")
		boolean lightmapDebug = false;

		@Comment("Summarizes multiple errors and warnings to single-line entries in the log.")
		boolean conciseErrors = true;

		@Comment("Writes information useful for bug reports to the game log at startup.")
		boolean logMachineInfo = true;

		@Comment("Writes OpenGL state changes to log.  *VERY SPAMMY - KILLS FRAME RATE*  Used only for debugging.")
		boolean logGlStateChanges = false;

		@Comment("Enables LWJGL memory allocation tracking.  Will harm performance. Use for debugging memory leaks. Requires restart.")
		boolean debugNativeMemoryAllocation = false;

		@Comment("Uses slower and safer memory allocation method for GL buffers.  Use only if having problems. Requires restart.")
		boolean safeNativeMemoryAllocation = false;

		@Comment("Output performance trade data to log. Will have significant performance impact. Requires restart.")
		boolean enablePerformanceTrace = false;

		@Comment("Output periodic snapshots of terrain occlusion raster. Will have performance impact.")
		boolean debugOcclusionRaster = false;

		@Comment("Render active occlusion boxes of targeted render region. Will have performance impact and looks strange.")
		boolean debugOcclusionBoxes = false;

		@Comment("Log clipping or other non-critical failures detected by terrain occluder. May spam the log.")
		boolean traceOcclusionEdgeCases = false;

		@Comment("Enable rendering of internal buffers for debug purposes. Off by default to prevent accidental activation.")
		public boolean enableBufferDebug;
	}

	static final ConfigData DEFAULTS = new ConfigData();
	private static final Gson GSON = new GsonBuilder().create();
	private static final Jankson JANKSON = Jankson.builder().build();

	public static FogMode fogMode = DEFAULTS.fogMode;
	public static boolean blendFluidColors = DEFAULTS.blendFluidColors;
	public static boolean enableBloom = DEFAULTS.enableBloom;
	public static float bloomIntensity = DEFAULTS.bloomIntensity;
	public static float bloomScale = DEFAULTS.bloomScale;

	private static boolean hdLightmaps = DEFAULTS.hdLightmaps;
	public static boolean lightmapNoise = DEFAULTS.lightmapNoise;
	public static DiffuseMode diffuseShadingMode = DEFAULTS.diffuseShadingMode;
	public static boolean lightSmoothing = DEFAULTS.lightSmoothing;
	public static AoMode aoShadingMode = DEFAULTS.aoShadingMode;
	public static boolean moreLightmap = DEFAULTS.moreLightmap;
	public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
	public static boolean semiFlatLighting = DEFAULTS.semiFlatLighting;

	public static boolean batchedChunkRender = DEFAULTS.batchedChunkRender;
	public static boolean preventDepthFighting = DEFAULTS.preventDepthFighting;
	public static boolean clampExteriorVertices = DEFAULTS.clampExteriorVertices;
	public static boolean fixLuminousBlockShading = DEFAULTS.fixLuminousBlockShading;
	public static boolean terrainSetupOffThread = DEFAULTS.terrainSetupOffThread;
	private static boolean enableVao = DEFAULTS.enableVao;
	public static boolean cullEntityRender = DEFAULTS.cullEntityRender;

	public static boolean shaderDebug = DEFAULTS.shaderDebug;
	public static boolean lightmapDebug = DEFAULTS.lightmapDebug;
	public static boolean conciseErrors = DEFAULTS.conciseErrors;
	public static boolean logMachineInfo = DEFAULTS.logMachineInfo;
	public static boolean logGlStateChanges = DEFAULTS.logGlStateChanges;
	public static boolean debugNativeMemoryAllocation = DEFAULTS.debugNativeMemoryAllocation;
	public static boolean safeNativeMemoryAllocation = DEFAULTS.safeNativeMemoryAllocation;
	public static boolean enablePerformanceTrace = DEFAULTS.enablePerformanceTrace;
	public static boolean debugOcclusionRaster = DEFAULTS.debugOcclusionRaster;
	public static boolean debugOcclusionBoxes = DEFAULTS.debugOcclusionBoxes;
	public static boolean traceOcclusionEdgeCases = DEFAULTS.traceOcclusionEdgeCases;
	public static boolean enableBufferDebug = DEFAULTS.enableBufferDebug;

	public static boolean hdLightmaps() {
		return false;
	}

	public static boolean enableVao() {
		return false;
	}

	/** use to stash parent screen during display */
	private static Screen screenIn;

	private static File configFile;

	public static void init() {
		configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "canvas.json5");
		if(configFile.exists()) {
			loadConfig();
		} else {
			saveConfig();
		}
	}

	private static void loadConfig() {
		ConfigData config = new ConfigData();
		try {
			final JsonObject configJson = JANKSON.load(configFile);
			final String regularized = configJson.toJson(false, false, 0);
			config = GSON.fromJson(regularized, ConfigData.class);
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Unable to load config. Using default values.");
		}

		fogMode = config.fogMode;
		blendFluidColors = config.blendFluidColors;
		enableBloom = config.enableBloom;
		bloomIntensity = config.bloomIntensity;
		bloomScale = config.bloomScale;

		shaderDebug = config.shaderDebug;
		maxLightmapDelayFrames = config.maxLightmapDelayFrames;
		moreLightmap = config.moreLightmap;

		hdLightmaps = config.hdLightmaps;
		lightmapNoise = config.lightmapNoise;
		diffuseShadingMode = config.diffuseShadingMode;
		lightSmoothing = config.lightSmoothing;
		aoShadingMode = config.aoShadingMode;
		semiFlatLighting = config.semiFlatLighting;

		batchedChunkRender = config.batchedChunkRender;
		//        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
		preventDepthFighting = config.preventDepthFighting;
		clampExteriorVertices = config.clampExteriorVertices;
		fixLuminousBlockShading = config.fixLuminousBlockShading;
		terrainSetupOffThread = config.terrainSetupOffThread;
		safeNativeMemoryAllocation = config.safeNativeMemoryAllocation;
		enableVao = config.enableVao;
		cullEntityRender = config.cullEntityRender;

		lightmapDebug = config.lightmapDebug;
		conciseErrors = config.conciseErrors;
		logMachineInfo = config.logMachineInfo;
		logGlStateChanges = config.logGlStateChanges;
		debugNativeMemoryAllocation = config.debugNativeMemoryAllocation;
		enablePerformanceTrace = config.enablePerformanceTrace;
		debugOcclusionBoxes = config.debugOcclusionBoxes;
		debugOcclusionRaster = config.debugOcclusionRaster;
		traceOcclusionEdgeCases = config.traceOcclusionEdgeCases;
		enableBufferDebug = config.enableBufferDebug;
	}

	private static void saveConfig() {
		final ConfigData config = new ConfigData();
		config.fogMode = fogMode;
		config.blendFluidColors = blendFluidColors;
		config.enableBloom = enableBloom;
		config.bloomIntensity = bloomIntensity;
		config.bloomScale = bloomScale;

		config.shaderDebug = shaderDebug;
		config.maxLightmapDelayFrames = maxLightmapDelayFrames;

		config.hdLightmaps = hdLightmaps;
		config.lightmapNoise = lightmapNoise;
		config.diffuseShadingMode = diffuseShadingMode;
		config.lightSmoothing = lightSmoothing;
		config.aoShadingMode = aoShadingMode;
		config.moreLightmap = moreLightmap;
		config.semiFlatLighting = semiFlatLighting;

		config.batchedChunkRender = batchedChunkRender;
		config.preventDepthFighting = preventDepthFighting;
		config.clampExteriorVertices = clampExteriorVertices;
		config.fixLuminousBlockShading = fixLuminousBlockShading;
		config.terrainSetupOffThread = terrainSetupOffThread;
		config.safeNativeMemoryAllocation = safeNativeMemoryAllocation;
		config.enableVao = enableVao;
		config.cullEntityRender = cullEntityRender;

		config.lightmapDebug = lightmapDebug;
		config.conciseErrors = conciseErrors;
		config.logMachineInfo = logMachineInfo;
		config.logGlStateChanges = logGlStateChanges;
		config.debugNativeMemoryAllocation = debugNativeMemoryAllocation;
		config.enablePerformanceTrace = enablePerformanceTrace;
		config.debugOcclusionBoxes = debugOcclusionBoxes;
		config.debugOcclusionRaster = debugOcclusionRaster;
		config.traceOcclusionEdgeCases = traceOcclusionEdgeCases;
		config.enableBufferDebug = enableBufferDebug;

		try {
			final String result = JANKSON.toJson(config).toJson(true, true, 0);
			if (!configFile.exists()) {
				configFile.createNewFile();
			}

			try(
					FileOutputStream out = new FileOutputStream(configFile, false);
					) {
				out.write(result.getBytes());
				out.flush();
				out.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			CanvasMod.LOG.error("Unable to save config.");
			return;
		}
	}

	static boolean reload = false;

	public enum AoMode {
		NORMAL,
		SUBTLE_ALWAYS,
		SUBTLE_BLOCK_LIGHT,
		NONE;

		@Override
		public String toString() {
			return I18n.translate("config.canvas.enum.ao_mode." + name().toLowerCase());
		}
	}

	public enum DiffuseMode {
		NORMAL,
		SKY_ONLY,
		NONE;

		@Override
		public String toString() {
			return I18n.translate("config.canvas.enum.diffuse_mode." + name().toLowerCase());
		}
	}

	public enum FogMode {
		VANILLA,
		SUBTLE,
		NONE;

		@Override
		public String toString() {
			return I18n.translate("config.canvas.enum.fog_mode." + name().toLowerCase());
		}
	}

	private static ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();

	static Text[] parse(String key) {
		return Arrays.stream(I18n.translate(key).split(";")).map(s ->  new LiteralText(s)).collect(Collectors.toList()).toArray(new Text[0]);
	}

	public static Screen display(Screen screen) {
		screenIn = screen;
		reload = false;

		final ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(screenIn)
				.setTitle(new TranslatableText("config.canvas.title"))
				.setSavingRunnable(Configurator::saveUserInput)
				.setAlwaysShowTabs(false)
				.setShouldListSmoothScroll(true)
				.setShouldListSmoothScroll(true);

		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(false);

		// FEATURES
		final ConfigCategory features = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.features"));

		features.addEntry(ENTRY_BUILDER
				.startEnumSelector(new TranslatableText("config.canvas.value.fog_mode"), FogMode.class, fogMode)
				.setDefaultValue(DEFAULTS.fogMode)
				.setTooltip(parse("config.canvas.help.fog_mode"))
				.setSaveConsumer(b -> {reload |= fogMode != b; fogMode = b;})
				.build());

		features.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.blend_fluid_colors"), blendFluidColors)
				.setDefaultValue(DEFAULTS.blendFluidColors)
				.setTooltip(parse("config.canvas.help.blend_fluid_colors"))
				.setSaveConsumer(b -> {reload |= blendFluidColors != b; blendFluidColors = b;})
				.build());

		features.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.bloom"), enableBloom)
				.setDefaultValue(DEFAULTS.enableBloom)
				.setTooltip(parse("config.canvas.help.bloom"))
				.setSaveConsumer(b -> {reload |= enableBloom != b; enableBloom = b;})
				.build());

		features.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.bloom_intensity"), (int)(bloomIntensity * 400), 0, 100)
				.setDefaultValue((int) (DEFAULTS.bloomIntensity * 400))
				.setMax(100)
				.setMin(0)
				.setTooltip(parse("config.canvas.help.bloom_intensity"))
				.setSaveConsumer(b -> bloomIntensity = b / 400f)
				.build());

		features.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.bloom_scale"), (int) (bloomScale * 100), 0, 200)
				.setDefaultValue((int) (DEFAULTS.bloomScale * 100))
				.setMax(200)
				.setMin(0)
				.setTooltip(parse("config.canvas.help.bloom_scale"))
				.setSaveConsumer(b -> bloomScale = b / 100f)
				.build());

		// LIGHTING
		final ConfigCategory lighting = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.lighting"));

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.light_smoothing"), lightSmoothing)
				.setDefaultValue(DEFAULTS.lightSmoothing)
				.setTooltip(parse("config.canvas.help.light_smoothing"))
				.setSaveConsumer(b -> {reload |= lightSmoothing != b; lightSmoothing = b;})
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.hd_lightmaps"), hdLightmaps)
				.setDefaultValue(DEFAULTS.hdLightmaps)
				.setTooltip(parse("config.canvas.help.hd_lightmaps"))
				.setSaveConsumer(b -> {reload |= hdLightmaps != b; hdLightmaps = b;})
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.more_lightmap"), moreLightmap)
				.setDefaultValue(DEFAULTS.moreLightmap)
				.setTooltip(parse("config.canvas.help.more_lightmap"))
				.setSaveConsumer(b -> moreLightmap = b)
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.lightmap_noise"), lightmapNoise)
				.setDefaultValue(DEFAULTS.lightmapNoise)
				.setTooltip(parse("config.canvas.help.lightmap_noise"))
				.setSaveConsumer(b -> {reload |= lightmapNoise != b; lightmapNoise = b;})
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startEnumSelector(new TranslatableText("config.canvas.value.diffuse_shading"), DiffuseMode.class, diffuseShadingMode)
				.setDefaultValue(DEFAULTS.diffuseShadingMode)
				.setTooltip(parse("config.canvas.help.diffuse_shading"))
				.setSaveConsumer(b -> {reload |= diffuseShadingMode != b; diffuseShadingMode = b;})
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startEnumSelector(new TranslatableText("config.canvas.value.ao_shading"), AoMode.class, aoShadingMode)
				.setDefaultValue(DEFAULTS.aoShadingMode)
				.setTooltip(parse("config.canvas.help.ao_shading"))
				.setSaveConsumer(b -> {reload |= aoShadingMode != b; aoShadingMode = b;})
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.lightmap_delay_frames"), maxLightmapDelayFrames, 0, 20)
				.setDefaultValue(DEFAULTS.maxLightmapDelayFrames)
				.setTooltip(parse("config.canvas.help.lightmap_delay_frames"))
				.setSaveConsumer(b -> maxLightmapDelayFrames = b)
				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.semi_flat_lighting"), semiFlatLighting)
				.setDefaultValue(DEFAULTS.semiFlatLighting)
				.setTooltip(parse("config.canvas.help.semi_flat_lighting"))
				.setSaveConsumer(b -> {reload |= semiFlatLighting != b; semiFlatLighting = b;})
				.build());

		// TWEAKS
		final ConfigCategory tweaks = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.tweaks"));

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.batch_chunk_render"), batchedChunkRender)
				.setDefaultValue(DEFAULTS.batchedChunkRender)
				.setTooltip(parse("config.canvas.help.batch_chunk_render"))
				.setSaveConsumer(b -> batchedChunkRender = b)
				.build());

		//        tweaks.addOption(new BooleanListEntry("config.canvas.value.vanilla_chunk_matrix", disableVanillaChunkMatrix, "config.canvas.reset",
		//                () -> DEFAULTS.disableVanillaChunkMatrix, b -> disableVanillaChunkMatrix = b,
		//                () -> Optional.of(parse("config.canvas.help.vanilla_chunk_matrix"))));

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.adjust_vanilla_geometry"), preventDepthFighting)
				.setDefaultValue(DEFAULTS.preventDepthFighting)
				.setTooltip(parse("config.canvas.help.adjust_vanilla_geometry"))
				.setSaveConsumer(b -> {reload |= preventDepthFighting != b; preventDepthFighting = b;})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.clamp_exterior_vertices"), clampExteriorVertices)
				.setDefaultValue(DEFAULTS.clampExteriorVertices)
				.setTooltip(parse("config.canvas.help.clamp_exterior_vertices"))
				.setSaveConsumer(b -> {reload |= clampExteriorVertices != b; clampExteriorVertices = b;})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.fix_luminous_block_shade"), fixLuminousBlockShading)
				.setDefaultValue(DEFAULTS.fixLuminousBlockShading)
				.setTooltip(parse("config.canvas.help.fix_luminous_block_shade"))
				.setSaveConsumer(b -> {reload |= fixLuminousBlockShading != b; fixLuminousBlockShading = b;})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.terrain_setup_off_thread"), terrainSetupOffThread)
				.setDefaultValue(DEFAULTS.terrainSetupOffThread)
				.setTooltip(parse("config.canvas.help.terrain_setup_off_thread"))
				.setSaveConsumer(b -> {reload |= terrainSetupOffThread != b; terrainSetupOffThread = b;})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.safe_native_allocation"), safeNativeMemoryAllocation)
				.setDefaultValue(DEFAULTS.safeNativeMemoryAllocation)
				.setTooltip(parse("config.canvas.help.safe_native_allocation"))
				.setSaveConsumer(b -> safeNativeMemoryAllocation = b)
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.enable_vao"), enableVao)
				.setDefaultValue(DEFAULTS.enableVao)
				.setTooltip(parse("config.canvas.help.enable_vao"))
				.setSaveConsumer(b -> {reload |= enableVao != b; enableVao = b;})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.cull_entity_render"), cullEntityRender)
				.setDefaultValue(DEFAULTS.cullEntityRender)
				.setTooltip(parse("config.canvas.help.enable_vao"))
				.setSaveConsumer(b -> {cullEntityRender = b;})
				.build());

		// DEBUG
		final ConfigCategory debug = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.debug"));

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.shader_debug"), shaderDebug)
				.setDefaultValue(DEFAULTS.shaderDebug)
				.setTooltip(parse("config.canvas.help.shader_debug"))
				.setSaveConsumer(b -> shaderDebug = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.shader_debug_lightmap"), lightmapDebug)
				.setDefaultValue(DEFAULTS.lightmapDebug)
				.setTooltip(parse("config.canvas.help.shader_debug_lightmap"))
				.setSaveConsumer(b -> lightmapDebug = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.concise_errors"), conciseErrors)
				.setDefaultValue(DEFAULTS.conciseErrors)
				.setTooltip(parse("config.canvas.help.concise_errors"))
				.setSaveConsumer(b -> conciseErrors = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.log_machine_info"), logMachineInfo)
				.setDefaultValue(DEFAULTS.logMachineInfo)
				.setTooltip(parse("config.canvas.help.log_machine_info"))
				.setSaveConsumer(b -> logMachineInfo = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.log_gl_state_changes"), logGlStateChanges)
				.setDefaultValue(DEFAULTS.logGlStateChanges)
				.setTooltip(parse("config.canvas.help.log_gl_state_changes"))
				.setSaveConsumer(b -> logGlStateChanges = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.debug_native_allocation"), debugNativeMemoryAllocation)
				.setDefaultValue(DEFAULTS.debugNativeMemoryAllocation)
				.setTooltip(parse("config.canvas.help.debug_native_allocation"))
				.setSaveConsumer(b -> debugNativeMemoryAllocation = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.debug_occlusion_raster"), debugOcclusionRaster)
				.setDefaultValue(DEFAULTS.debugOcclusionRaster)
				.setTooltip(parse("config.canvas.help.debug_occlusion_raster"))
				.setSaveConsumer(b -> debugOcclusionRaster = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.debug_occlusion_boxes"), debugOcclusionBoxes)
				.setDefaultValue(DEFAULTS.debugOcclusionBoxes)
				.setTooltip(parse("config.canvas.help.debug_occlusion_boxes"))
				.setSaveConsumer(b -> debugOcclusionBoxes = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.trace_occlusion_edge_cases"), traceOcclusionEdgeCases)
				.setDefaultValue(DEFAULTS.traceOcclusionEdgeCases)
				.setTooltip(parse("config.canvas.help.trace_occlusion_edge_cases"))
				.setSaveConsumer(b -> traceOcclusionEdgeCases = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.buffer_debug"), enableBufferDebug)
				.setDefaultValue(DEFAULTS.enableBufferDebug)
				.setTooltip(parse("config.canvas.help.buffer_debug"))
				.setSaveConsumer(b -> enableBufferDebug = b)
				.build());

		builder.setAlwaysShowTabs(false).setDoesConfirmSave(false);

		return builder.build();
	}

	@SuppressWarnings("resource")
	private static void saveUserInput() {
		saveConfig();

		if(reload) {
			Canvas.INSTANCE.reload();
			MinecraftClient.getInstance().worldRenderer.reload();
		}
	}

	// LEGACY STUFF

	//    @LangKey("config.acuity_fancy_fluids")
	//    @Comment({"Enable fancy water and lava rendering.",
	//        " This feature is currently work in progress and has no visible effect if enabled."})
	public static boolean fancyFluids = false;


	//    @LangKey("config.disable_yield")
	//    @RequiresMcRestart
	//    @Comment({"When enabled, disables the call to Thread.yield() in the main game loop ",
	//        " that normally occurs right after display update. The call is probably meant",
	//        " to give the OpenGL drivers time to process the command buffer, but in the multi-threaded game ",
	//        " Minecraft has become, and with modern drivers, this basically invites other tasks to step on your framerate.",
	//        " This patch is purely a performance optimization and is not required for Acuity to operate."})
	//    public static boolean disableYieldInGameLoop = true;

	//    public static void handleChange() // PostConfigChangedEvent event)
	//    {
	//        boolean oldFancyFluids = fancyFluids;
	//        boolean oldVAO = enable_vao;
	//        ConfigManager.sync(Acuity.MODID, Config.Type.INSTANCE);
	//        if (oldFancyFluids != fancyFluids || oldVAO != enable_vao) {
	//            RendererImpl.INSTANCE.reload();
	//
	//            // refresh appearances
	//            MinecraftClient.getInstance().worldRenderer.reload();
	//        }
	//    }

}
