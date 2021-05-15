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

package grondag.canvas.config;

import static grondag.canvas.config.ConfigManager.DEFAULTS;
import static grondag.canvas.config.ConfigManager.parse;
import static grondag.canvas.config.Configurator.batchedChunkRender;
import static grondag.canvas.config.Configurator.blendFluidColors;
import static grondag.canvas.config.Configurator.clampExteriorVertices;
import static grondag.canvas.config.Configurator.conciseErrors;
import static grondag.canvas.config.Configurator.cullEntityRender;
import static grondag.canvas.config.Configurator.cullParticles;
import static grondag.canvas.config.Configurator.debugNativeMemoryAllocation;
import static grondag.canvas.config.Configurator.debugOcclusionBoxes;
import static grondag.canvas.config.Configurator.debugOcclusionRaster;
import static grondag.canvas.config.Configurator.displayRenderProfiler;
import static grondag.canvas.config.Configurator.dynamicFrustumPadding;
import static grondag.canvas.config.Configurator.enableBufferDebug;
import static grondag.canvas.config.Configurator.enableLifeCycleDebug;
import static grondag.canvas.config.Configurator.enableVao;
import static grondag.canvas.config.Configurator.fixLuminousBlockShading;
import static grondag.canvas.config.Configurator.forceJmxModelLoading;
import static grondag.canvas.config.Configurator.greedyRenderThread;
import static grondag.canvas.config.Configurator.lightSmoothing;
import static grondag.canvas.config.Configurator.logGlStateChanges;
import static grondag.canvas.config.Configurator.logMachineInfo;
import static grondag.canvas.config.Configurator.logMaterials;
import static grondag.canvas.config.Configurator.logMissingUniforms;
import static grondag.canvas.config.Configurator.logRenderLagSpikes;
import static grondag.canvas.config.Configurator.pipelineId;
import static grondag.canvas.config.Configurator.preventDepthFighting;
import static grondag.canvas.config.Configurator.profileProcessShaders;
import static grondag.canvas.config.Configurator.profilerDetailLevel;
import static grondag.canvas.config.Configurator.profilerOverlayScale;
import static grondag.canvas.config.Configurator.reduceResolutionOnMac;
import static grondag.canvas.config.Configurator.reload;
import static grondag.canvas.config.Configurator.renderLagSpikeFps;
import static grondag.canvas.config.Configurator.safeNativeMemoryAllocation;
import static grondag.canvas.config.Configurator.semiFlatLighting;
import static grondag.canvas.config.Configurator.shaderDebug;
import static grondag.canvas.config.Configurator.staticFrustumPadding;
import static grondag.canvas.config.Configurator.terrainSetupOffThread;
import static grondag.canvas.config.Configurator.traceOcclusionEdgeCases;
import static grondag.canvas.config.Configurator.traceOcclusionOutcomes;
import static grondag.canvas.config.Configurator.vertexControlMode;
import static grondag.canvas.config.Configurator.wavyGrass;

import java.lang.ref.WeakReference;
import java.util.Optional;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.pipeline.config.PipelineDescription;
import grondag.canvas.pipeline.config.PipelineLoader;

public class ConfigGui {
	static final ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();

	/**
	 * Use to stash parent screen during display.
	 */
	private static WeakReference<Screen> configScreen;
	private static SelectionListEntry<PipelineDescription> pipeline;

	static Identifier pipeline() {
		return pipeline == null ? PipelineConfig.DEFAULT_ID : pipeline.getValue().id;
	}

	static Screen current() {
		return configScreen.get();
	}

	public static Screen display(Screen parent) {
		reload = false;

		final ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(new TranslatableText("config.canvas.title"))
				.setSavingRunnable(ConfigManager::saveUserInput)
				.setAlwaysShowTabs(false)
				.setShouldListSmoothScroll(true)
				.setShouldListSmoothScroll(true);

		builder.setGlobalized(true);
		builder.setGlobalizedExpanded(false);

		// FEATURES
		final ConfigCategory features = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.features"));

		pipeline = ENTRY_BUILDER
				.startSelector(new TranslatableText("config.canvas.value.pipeline"), PipelineLoader.array(), PipelineLoader.get(pipelineId))
				.setNameProvider(o -> new LiteralText(o.name()))
				.setTooltip(parse("config.canvas.help.pipeline"))
				.setTooltipSupplier(o -> Optional.of(parse(o.descriptionKey)))
				.setSaveConsumer(b -> {
					if (!b.id.toString().equals(pipelineId)) {
						Pipeline.reload();
						reload = true;
						pipelineId = b.id.toString();
					}
				})
				.build();

		features.addEntry(pipeline);

		features.addEntry(new PipelineOptionsEntry());

		features.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.blend_fluid_colors"), blendFluidColors)
				.setDefaultValue(DEFAULTS.blendFluidColors)
				.setTooltip(parse("config.canvas.help.blend_fluid_colors"))
				.setSaveConsumer(b -> {
					reload |= blendFluidColors != b;
					blendFluidColors = b;
				})
				.build());

		features.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.wavy_grass"), wavyGrass)
				.setDefaultValue(DEFAULTS.wavyGrass)
				.setTooltip(parse("config.canvas.help.wavy_grass"))
				.setSaveConsumer(b -> {
					reload |= wavyGrass != b;
					wavyGrass = b;
				})
				.build());

		// LIGHTING
		final ConfigCategory lighting = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.lighting"));

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.light_smoothing"), lightSmoothing)
				.setDefaultValue(DEFAULTS.lightSmoothing)
				.setTooltip(parse("config.canvas.help.light_smoothing"))
				.setSaveConsumer(b -> {
					reload |= lightSmoothing != b;
					lightSmoothing = b;
				})
				.build());

		//		lighting.addEntry(ENTRY_BUILDER
		//				.startBooleanToggle(new TranslatableText("config.canvas.value.hd_lightmaps"), hdLightmaps)
		//				.setDefaultValue(DEFAULTS.hdLightmaps)
		//				.setTooltip(parse("config.canvas.help.hd_lightmaps"))
		//				.setSaveConsumer(b -> {
		//					reload |= hdLightmaps != b;
		//					hdLightmaps = b;
		//				})
		//				.build());

		//		lighting.addEntry(ENTRY_BUILDER
		//				.startBooleanToggle(new TranslatableText("config.canvas.value.more_lightmap"), moreLightmap)
		//				.setDefaultValue(DEFAULTS.moreLightmap)
		//				.setTooltip(parse("config.canvas.help.more_lightmap"))
		//				.setSaveConsumer(b -> moreLightmap = b)
		//				.build());

		//		lighting.addEntry(ENTRY_BUILDER
		//				.startBooleanToggle(new TranslatableText("config.canvas.value.lightmap_noise"), lightmapNoise)
		//				.setDefaultValue(DEFAULTS.lightmapNoise)
		//				.setTooltip(parse("config.canvas.help.lightmap_noise"))
		//				.setSaveConsumer(b -> {
		//					reload |= lightmapNoise != b;
		//					lightmapNoise = b;
		//				})
		//				.build());

		//		lighting.addEntry(ENTRY_BUILDER
		//				.startIntSlider(new TranslatableText("config.canvas.value.lightmap_delay_frames"), maxLightmapDelayFrames, 0, 20)
		//				.setDefaultValue(DEFAULTS.maxLightmapDelayFrames)
		//				.setTooltip(parse("config.canvas.help.lightmap_delay_frames"))
		//				.setSaveConsumer(b -> maxLightmapDelayFrames = b)
		//				.build());

		lighting.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.semi_flat_lighting"), semiFlatLighting)
				.setDefaultValue(DEFAULTS.semiFlatLighting)
				.setTooltip(parse("config.canvas.help.semi_flat_lighting"))
				.setSaveConsumer(b -> {
					reload |= semiFlatLighting != b;
					semiFlatLighting = b;
				})
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
				.setSaveConsumer(b -> {
					reload |= preventDepthFighting != b;
					preventDepthFighting = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.clamp_exterior_vertices"), clampExteriorVertices)
				.setDefaultValue(DEFAULTS.clampExteriorVertices)
				.setTooltip(parse("config.canvas.help.clamp_exterior_vertices"))
				.setSaveConsumer(b -> {
					reload |= clampExteriorVertices != b;
					clampExteriorVertices = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.fix_luminous_block_shade"), fixLuminousBlockShading)
				.setDefaultValue(DEFAULTS.fixLuminousBlockShading)
				.setTooltip(parse("config.canvas.help.fix_luminous_block_shade"))
				.setSaveConsumer(b -> {
					reload |= fixLuminousBlockShading != b;
					fixLuminousBlockShading = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.terrain_setup_off_thread"), terrainSetupOffThread)
				.setDefaultValue(DEFAULTS.terrainSetupOffThread)
				.setTooltip(parse("config.canvas.help.terrain_setup_off_thread"))
				.setSaveConsumer(b -> {
					reload |= terrainSetupOffThread != b;
					terrainSetupOffThread = b;
				})
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
				.setSaveConsumer(b -> {
					reload |= enableVao != b;
					enableVao = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.cull_entity_render"), cullEntityRender)
				.setDefaultValue(DEFAULTS.cullEntityRender)
				.setTooltip(parse("config.canvas.help.cull_entity_render"))
				.setSaveConsumer(b -> {
					cullEntityRender = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.greedy_render_thread"), greedyRenderThread)
				.setDefaultValue(DEFAULTS.greedyRenderThread)
				.setTooltip(parse("config.canvas.help.greedy_render_thread"))
				.setSaveConsumer(b -> {
					greedyRenderThread = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.force_jmx_loading"), forceJmxModelLoading)
				.setDefaultValue(DEFAULTS.forceJmxModelLoading)
				.setTooltip(parse("config.canvas.help.force_jmx_loading"))
				.setSaveConsumer(b -> {
					forceJmxModelLoading = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.reduce_resolution_on_mac"), reduceResolutionOnMac)
				.setDefaultValue(DEFAULTS.reduceResolutionOnMac)
				.setTooltip(parse("config.canvas.help.reduce_resolution_on_mac"))
				.setSaveConsumer(b -> {
					reduceResolutionOnMac = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.vertex_control_mode"), vertexControlMode)
				.setDefaultValue(DEFAULTS.vertexControlMode)
				.setTooltip(parse("config.canvas.help.vertex_control_mode"))
				.requireRestart()
				.setSaveConsumer(b -> {
					vertexControlMode = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.static_frustum_padding"), staticFrustumPadding, 0, 20)
				.setDefaultValue(DEFAULTS.staticFrustumPadding)
				.setTooltip(parse("config.canvas.help.static_frustum_padding"))
				.setSaveConsumer(b -> {
					staticFrustumPadding = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.dynamic_frustum_padding"), dynamicFrustumPadding, 0, 30)
				.setDefaultValue(DEFAULTS.dynamicFrustumPadding)
				.setTooltip(parse("config.canvas.help.dynamic_frustum_padding"))
				.setSaveConsumer(b -> {
					dynamicFrustumPadding = b;
				})
				.build());

		tweaks.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.cull_particles"), cullParticles)
				.setDefaultValue(DEFAULTS.cullParticles)
				.setTooltip(parse("config.canvas.help.cull_particles"))
				.setSaveConsumer(b -> {
					cullParticles = b;
				})
				.build());

		// DEBUG
		final ConfigCategory debug = builder.getOrCreateCategory(new TranslatableText("config.canvas.category.debug"));

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.shader_debug"), shaderDebug)
				.setDefaultValue(DEFAULTS.shaderDebug)
				.setTooltip(parse("config.canvas.help.shader_debug"))
				.setSaveConsumer(b -> shaderDebug = b)
				.build());

		//		debug.addEntry(ENTRY_BUILDER
		//				.startBooleanToggle(new TranslatableText("config.canvas.value.shader_debug_lightmap"), lightmapDebug)
		//				.setDefaultValue(DEFAULTS.lightmapDebug)
		//				.setTooltip(parse("config.canvas.help.shader_debug_lightmap"))
		//				.setSaveConsumer(b -> lightmapDebug = b)
		//				.build());

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
				.startBooleanToggle(new TranslatableText("config.canvas.value.trace_occlusion_outcomes"), traceOcclusionOutcomes)
				.setDefaultValue(DEFAULTS.traceOcclusionOutcomes)
				.requireRestart()
				.setTooltip(parse("config.canvas.help.trace_occlusion_outcomes"))
				.setSaveConsumer(b -> traceOcclusionOutcomes = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.buffer_debug"), enableBufferDebug)
				.setDefaultValue(DEFAULTS.enableBufferDebug)
				.setTooltip(parse("config.canvas.help.buffer_debug"))
				.setSaveConsumer(b -> enableBufferDebug = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.lifecycle_debug"), enableLifeCycleDebug)
				.setDefaultValue(DEFAULTS.enableLifeCycleDebug)
				.setTooltip(parse("config.canvas.help.lifecycle_debug"))
				.setSaveConsumer(b -> enableLifeCycleDebug = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.log_missing_uniforms"), logMissingUniforms)
				.setDefaultValue(DEFAULTS.logMissingUniforms)
				.setTooltip(parse("config.canvas.help.log_missing_uniforms"))
				.setSaveConsumer(b -> logMissingUniforms = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.log_materials"), logMaterials)
				.setDefaultValue(DEFAULTS.logMaterials)
				.setTooltip(parse("config.canvas.help.log_materials"))
				.setSaveConsumer(b -> logMaterials = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startBooleanToggle(new TranslatableText("config.canvas.value.log_render_lag_spikes"), logRenderLagSpikes)
				.setDefaultValue(DEFAULTS.logRenderLagSpikes)
				.setTooltip(parse("config.canvas.help.log_render_lag_spikes"))
				.setSaveConsumer(b -> {
					logRenderLagSpikes = b;
					Timekeeper.configOrPipelineReload();
				})
				.build());

		debug.addEntry(ENTRY_BUILDER
				.startIntSlider(new TranslatableText("config.canvas.value.render_lag_spike_fps"), renderLagSpikeFps, 30, 120)
				.setDefaultValue(DEFAULTS.renderLagSpikeFps)
				.setTooltip(parse("config.canvas.help.render_lag_spike_fps"))
				.setSaveConsumer(b -> renderLagSpikeFps = b)
				.build());

		debug.addEntry(ENTRY_BUILDER
			.startBooleanToggle(new TranslatableText("config.canvas.value.display_render_profiler"), displayRenderProfiler)
			.setDefaultValue(DEFAULTS.displayRenderProfiler)
			.setTooltip(parse("config.canvas.help.display_render_profiler"))
			.setSaveConsumer(b -> {
				displayRenderProfiler = b;
				Timekeeper.configOrPipelineReload();
			})
			.build());

		debug.addEntry(ENTRY_BUILDER
			.startBooleanToggle(new TranslatableText("config.canvas.value.profile_process_shaders"), profileProcessShaders)
			.setDefaultValue(DEFAULTS.profileProcessShaders)
			.setTooltip(parse("config.canvas.help.profile_process_shaders"))
			.setSaveConsumer(b -> {
				profileProcessShaders = b;
				Timekeeper.configOrPipelineReload();
			})
			.build());

		debug.addEntry(ENTRY_BUILDER
			.startIntSlider(new TranslatableText("config.canvas.value.profiler_detail_level"), profilerDetailLevel, 0, 2)
			.setDefaultValue(DEFAULTS.profilerDetailLevel)
			.setTooltip(parse("config.canvas.help.profiler_detail_level"))
			.setSaveConsumer(b -> profilerDetailLevel = b)
			.build());

		debug.addEntry(ENTRY_BUILDER
			.startFloatField(new TranslatableText("config.canvas.value.profiler_overlay_scale"), profilerOverlayScale)
			.setDefaultValue(DEFAULTS.profilerOverlayScale)
			.setTooltip(parse("config.canvas.help.profiler_overlay_scale"))
			.setSaveConsumer(b -> profilerOverlayScale = b)
			.build());

		builder.setAlwaysShowTabs(false).setDoesConfirmSave(false);

		final Screen result = builder.build();
		configScreen = new WeakReference<>(result);
		return result;
	}
}
