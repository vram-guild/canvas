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

package grondag.canvas.config;

import static grondag.canvas.config.ConfigManager.DEFAULTS;
import static grondag.canvas.config.ConfigManager.Reload.DONT_RELOAD;
import static grondag.canvas.config.ConfigManager.Reload.RELOAD_EVERYTHING;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.config.builder.Buttons;
import grondag.canvas.config.builder.OptionSession;
import grondag.canvas.config.gui.ActionItem;
import grondag.canvas.config.gui.BaseScreen;
import grondag.canvas.config.gui.ListWidget;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.render.world.SkyShadowRenderer;
import grondag.canvas.terrain.occlusion.TerrainIterator;

public class CanvasConfigScreen extends BaseScreen {
	private boolean reload;
	private boolean reloadTimekeeper;
	private boolean requiresRestart;

	private final ConfigData editing;
	private final OptionSession optionSession = new OptionSession();

	private ListWidget list;

	public CanvasConfigScreen(Screen parent) {
		super(parent, new TranslatableComponent("config.canvas.title"));

		reload = false;
		reloadTimekeeper = false;
		requiresRestart = false;

		editing = new ConfigData();
		Configurator.writeToConfig(editing);
	}

	@Override
	protected void init() {
		super.init();

		final int sideW = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;
		final int rightSideW = Math.min(sideW, Math.max(0, this.width - 330 - sideW));

		list = new ListWidget(sideW + 2, 22 + 2, this.width - sideW - 4 - rightSideW, this.height - 35 - 22);
		addRenderableWidget(list);

		list.addItem(new ActionItem("config.canvas.value.pipeline_config", Buttons.BrowseButton::new,
				() -> minecraft.setScreen(new PipelineOptionScreen(this, new ResourceLocation(Configurator.pipelineId)))));

		// FEATURES
		@SuppressWarnings("unused")
		final int indexFeatures = list.addCategory("config.canvas.category.features");

		list.addItem(optionSession.booleanOption("config.canvas.value.blend_fluid_colors",
				() -> editing.blendFluidColors,
				b -> {
					reload |= Configurator.blendFluidColors != b;
					editing.blendFluidColors = b;
				},
				DEFAULTS.blendFluidColors,
				"config.canvas.help.blend_fluid_colors").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.wavy_grass",
				() -> editing.wavyGrass,
				b -> {
					reload |= Configurator.wavyGrass != b;
					editing.wavyGrass = b;
				},
				DEFAULTS.wavyGrass,
				"config.canvas.help.wavy_grass").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.disable_vignette",
				() -> editing.disableVignette,
				b -> editing.disableVignette = b,
				DEFAULTS.disableVignette,
				"config.canvas.help.disable_vignette").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.semi_flat_lighting",
				() -> editing.semiFlatLighting,
				b -> {
					reload |= Configurator.semiFlatLighting != b;
					editing.semiFlatLighting = b;
				},
				DEFAULTS.semiFlatLighting,
				"config.canvas.help.semi_flat_lighting").listItem());

		// TWEAKS
		final int indexTweaks = list.addCategory("config.canvas.category.tweaks");

		list.addItem(optionSession.booleanOption("config.canvas.value.adjust_vanilla_geometry",
				() -> editing.preventDepthFighting,
				b -> {
					reload |= Configurator.preventDepthFighting != b;
					editing.preventDepthFighting = b;
				},
				DEFAULTS.preventDepthFighting,
				"config.canvas.help.adjust_vanilla_geometry").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.clamp_exterior_vertices",
				() -> editing.clampExteriorVertices,
				b -> {
					reload |= Configurator.clampExteriorVertices != b;
					editing.clampExteriorVertices = b;
				},
				DEFAULTS.clampExteriorVertices,
				"config.canvas.help.clamp_exterior_vertices").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.fix_luminous_block_shade",
				() -> editing.fixLuminousBlockShading,
				b -> {
					reload |= Configurator.fixLuminousBlockShading != b;
					editing.fixLuminousBlockShading = b;
				},
				DEFAULTS.fixLuminousBlockShading,
				"config.canvas.help.fix_luminous_block_shade").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.advanced_terrain_culling",
				() -> editing.advancedTerrainCulling,
				b -> {
					reload |= Configurator.advancedTerrainCulling != b;
					editing.advancedTerrainCulling = b;
				},
				DEFAULTS.advancedTerrainCulling,
				"config.canvas.help.advanced_terrain_culling").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.terrain_setup_off_thread",
				() -> editing.terrainSetupOffThread,
				b -> {
					reload |= Configurator.terrainSetupOffThread != b;
					editing.terrainSetupOffThread = b;
				},
				DEFAULTS.terrainSetupOffThread,
				"config.canvas.help.terrain_setup_off_thread").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.safe_native_allocation",
				() -> editing.safeNativeMemoryAllocation,
				b -> {
					requiresRestart |= Configurator.safeNativeMemoryAllocation.get() != b;
					editing.safeNativeMemoryAllocation = b;
				},
				Configurator.safeNativeMemoryAllocation,
				DEFAULTS.safeNativeMemoryAllocation,
				"config.canvas.help.safe_native_allocation").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.cull_entity_render",
				() -> editing.cullEntityRender,
				b -> editing.cullEntityRender = b,
				DEFAULTS.cullEntityRender,
				"config.canvas.help.cull_entity_render").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.greedy_render_thread",
				() -> editing.greedyRenderThread,
				b -> editing.greedyRenderThread = b,
				DEFAULTS.greedyRenderThread,
				"config.canvas.help.greedy_render_thread").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.force_jmx_loading",
				() -> editing.forceJmxModelLoading,
				b -> editing.forceJmxModelLoading = b,
				DEFAULTS.forceJmxModelLoading,
				"config.canvas.help.force_jmx_loading").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.reduce_resolution_on_mac",
				() -> editing.reduceResolutionOnMac,
				b -> {
					requiresRestart |= Configurator.reduceResolutionOnMac.get() != b;
					editing.reduceResolutionOnMac = b;
				},
				Configurator.reduceResolutionOnMac,
				DEFAULTS.reduceResolutionOnMac,
				"config.canvas.help.reduce_resolution_on_mac").listItem());

		list.addItem(optionSession.intOption("config.canvas.value.static_frustum_padding",
				0,
				20,
				1,
				() -> editing.staticFrustumPadding,
				i -> editing.staticFrustumPadding = i,
				DEFAULTS.staticFrustumPadding,
				"config.canvas.help.static_frustum_padding").listItem());

		list.addItem(optionSession.intOption("config.canvas.value.dynamic_frustum_padding",
				0,
				30,
				1,
				() -> editing.dynamicFrustumPadding,
				i -> editing.dynamicFrustumPadding = i,
				DEFAULTS.dynamicFrustumPadding,
				"config.canvas.help.dynamic_frustum_padding").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.cull_particles",
				() -> editing.cullParticles,
				b -> editing.cullParticles = b,
				DEFAULTS.cullParticles,
				"config.canvas.help.cull_particles").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.enable_near_occluders",
				() -> editing.enableNearOccluders,
				b -> editing.enableNearOccluders = b,
				DEFAULTS.enableNearOccluders,
				"config.canvas.help.enable_near_occluders").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.use_combined_thread_pool",
				() -> editing.useCombinedThreadPool,
				b -> {
					requiresRestart |= Configurator.useCombinedThreadPool.get() != b;
					editing.useCombinedThreadPool = b;
				},
				Configurator.useCombinedThreadPool,
				DEFAULTS.useCombinedThreadPool,
				"config.canvas.help.use_combined_thread_pool").listItem());

		list.addItem(optionSession.enumOption("config.canvas.value.transfer_buffer_mode",
				() -> editing.transferBufferMode,
				e -> {
					reload |= Configurator.transferBufferMode != e;
					editing.transferBufferMode = e;
				},
				DEFAULTS.transferBufferMode,
				TransferBuffers.Config.class,
				"config.canvas.help.transfer_buffer_mode").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.steady_debug_screen",
				() -> editing.steadyDebugScreen,
				b -> editing.steadyDebugScreen = b,
				DEFAULTS.steadyDebugScreen,
				"config.canvas.help.steady_debug_screen").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.disable_unseen_sprite_animation",
				() -> editing.disableUnseenSpriteAnimation,
				b -> {
					reload |= Configurator.disableUnseenSpriteAnimation != b;
					editing.disableUnseenSpriteAnimation = b;
				},
				DEFAULTS.disableUnseenSpriteAnimation,
				"config.canvas.help.disable_unseen_sprite_animation").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.cull_backfacing_terrain",
				() -> editing.cullBackfacingTerrain,
				b -> {
					reload |= Configurator.cullBackfacingTerrain != b;
					editing.cullBackfacingTerrain = b;
				},
				DEFAULTS.cullBackfacingTerrain,
				"config.canvas.help.cull_backfacing_terrain").listItem());

		// DEBUG
		final int indexDebug = list.addCategory("config.canvas.category.debug");

		list.addItem(optionSession.enumOption("config.canvas.value.shadow_priming_strategy",
				() -> editing.shadowPrimingStrategy,
				e -> {
					reload |= Configurator.shadowPrimingStrategy != e;
					editing.shadowPrimingStrategy = e;
				},
				DEFAULTS.shadowPrimingStrategy,
				TerrainIterator.ShadowPriming.class,
				"config.canvas.help.shadow_priming_strategy").listItem());

		list.addItem(optionSession.intOption("config.canvas.value.shadow_max_distance",
				4,
				32,
				1,
				() -> editing.shadowMaxDistance,
				i -> editing.shadowMaxDistance = i,
				DEFAULTS.shadowMaxDistance,
				"config.canvas.help.shadow_max_distance").listItem());

		list.addItem(optionSession.enumOption("config.canvas.value.shadow_face_culling",
				() -> editing.shadowFaceCulling,
				e -> {
					reload |= Configurator.shadowFaceCulling != e;
					editing.shadowFaceCulling = e;
				},
				DEFAULTS.shadowFaceCulling,
				SkyShadowRenderer.Culling.class,
				"config.canvas.help.shadow_face_culling").listItem());

		list.addItem(optionSession.floatOption("config.canvas.value.shadow_center_factor",
				0.0f,
				1.0f,
				0.1f,
				() -> editing.shadowCenterFactor,
				f -> editing.shadowCenterFactor = f,
				DEFAULTS.shadowCenterFactor,
				"config.canvas.help.shadow_center_factor").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.disable_shadow_self_occlusion",
				() -> editing.disableShadowSelfOcclusion,
				b -> editing.disableShadowSelfOcclusion = b,
				DEFAULTS.disableShadowSelfOcclusion,
				"config.canvas.help.disable_shadow_self_occlusion").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.shader_debug",
				() -> editing.shaderDebug,
				b -> editing.shaderDebug = b,
				DEFAULTS.shaderDebug,
				"config.canvas.help.shader_debug").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.preprocess_shader_source",
				() -> editing.preprocessShaderSource,
				b -> {
					reload |= Configurator.preprocessShaderSource != b;
					editing.preprocessShaderSource = b;
				},
				DEFAULTS.preprocessShaderSource,
				"config.canvas.help.preprocess_shader_source").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.concise_errors",
				() -> editing.conciseErrors,
				b -> editing.conciseErrors = b,
				DEFAULTS.conciseErrors,
				"config.canvas.help.concise_errors").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.log_machine_info",
				() -> editing.logMachineInfo,
				b -> editing.logMachineInfo = b,
				DEFAULTS.logMachineInfo,
				"config.canvas.help.log_machine_info").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.log_gl_state_changes",
				() -> editing.logGlStateChanges,
				b -> editing.logGlStateChanges = b,
				DEFAULTS.logGlStateChanges,
				"config.canvas.help.log_gl_state_changes").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.debug_native_allocation",
				() -> editing.debugNativeMemoryAllocation,
				b -> {
					requiresRestart |= Configurator.debugNativeMemoryAllocation.get() != b;
					editing.debugNativeMemoryAllocation = b;
				},
				Configurator.debugNativeMemoryAllocation,
				DEFAULTS.debugNativeMemoryAllocation,
				"config.canvas.help.debug_native_allocation").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.debug_occlusion_raster",
				() -> editing.debugOcclusionRaster,
				b -> editing.debugOcclusionRaster = b,
				DEFAULTS.debugOcclusionRaster,
				"config.canvas.help.debug_occlusion_raster").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.debug_occlusion_boxes",
				() -> editing.debugOcclusionBoxes,
				b -> editing.debugOcclusionBoxes = b,
				DEFAULTS.debugOcclusionBoxes,
				"config.canvas.help.debug_occlusion_boxes").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.white_glass_occludes_terrain",
				() -> editing.renderWhiteGlassAsOccluder,
				b -> {
					reload |= Configurator.renderWhiteGlassAsOccluder != b;
					editing.renderWhiteGlassAsOccluder = b;
				},
				DEFAULTS.renderWhiteGlassAsOccluder,
				"config.canvas.help.white_glass_occludes_terrain").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.trace_occlusion_edge_cases",
				() -> editing.traceOcclusionEdgeCases,
				b -> editing.traceOcclusionEdgeCases = b,
				DEFAULTS.traceOcclusionEdgeCases,
				"config.canvas.help.trace_occlusion_edge_cases").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.buffer_debug",
				() -> editing.enableBufferDebug,
				b -> editing.enableBufferDebug = b,
				DEFAULTS.enableBufferDebug,
				"config.canvas.help.buffer_debug").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.lifecycle_debug",
				() -> editing.enableLifeCycleDebug,
				b -> editing.enableLifeCycleDebug = b,
				DEFAULTS.enableLifeCycleDebug,
				"config.canvas.help.lifecycle_debug").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.log_missing_uniforms",
				() -> editing.logMissingUniforms,
				b -> editing.logMissingUniforms = b,
				DEFAULTS.logMissingUniforms,
				"config.canvas.help.log_missing_uniforms").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.log_materials",
				() -> editing.logMaterials,
				b -> editing.logMaterials = b,
				DEFAULTS.logMaterials,
				"config.canvas.help.log_materials").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.log_render_lag_spikes",
				() -> editing.logRenderLagSpikes,
				b -> {
					reloadTimekeeper |= Configurator.logRenderLagSpikes != b;
					editing.logRenderLagSpikes = b;
				},
				DEFAULTS.logRenderLagSpikes,
				"config.canvas.help.log_render_lag_spikes").listItem());

		list.addItem(optionSession.intOption("config.canvas.value.render_lag_spike_fps",
				30,
				120,
				1,
				() -> editing.renderLagSpikeFps,
				i -> editing.renderLagSpikeFps = i,
				DEFAULTS.renderLagSpikeFps,
				"config.canvas.help.render_lag_spike_fps").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.display_render_profiler",
				() -> editing.displayRenderProfiler,
				b -> {
					reloadTimekeeper |= Configurator.displayRenderProfiler != b;
					editing.displayRenderProfiler = b;
				},
				DEFAULTS.displayRenderProfiler,
				"config.canvas.help.display_render_profiler").listItem());

		list.addItem(optionSession.enumOption("config.canvas.value.profiler_display_mode",
				() -> editing.profilerDisplayMode,
				e -> {
					reloadTimekeeper |= Configurator.profilerDisplayMode != e;
					editing.profilerDisplayMode = e;
				},
				DEFAULTS.profilerDisplayMode,
				Timekeeper.Mode.class,
				"config.canvas.help.profiler_display_mode").listItem());

		list.addItem(optionSession.intOption("config.canvas.value.profiler_detail_level",
				0,
				2,
				1,
				() -> editing.profilerDetailLevel,
				i -> editing.profilerDetailLevel = i,
				DEFAULTS.profilerDetailLevel,
				"config.canvas.help.profiler_detail_level").listItem());

		list.addItem(optionSession.floatOption("config.canvas.value.profiler_overlay_scale",
				0.0f,
				1.0f,
				0.1f,
				() -> editing.profilerOverlayScale,
				f -> editing.profilerOverlayScale = f,
				DEFAULTS.profilerOverlayScale,
				"config.canvas.help.profiler_overlay_scale").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.debug_sprite_atlas",
				() -> editing.debugSpriteAtlas,
				b -> editing.debugSpriteAtlas = b,
				DEFAULTS.debugSpriteAtlas,
				"config.canvas.help.debug_sprite_atlas").listItem());

		list.addItem(optionSession.booleanOption("config.canvas.value.trace_texture_load",
				() -> editing.traceTextureLoad,
				b -> editing.traceTextureLoad = b,
				DEFAULTS.traceTextureLoad,
				"config.canvas.help.trace_texture_load").listItem());

		if (sideW > 0) {
			final ListWidget tabs = new ListWidget(1, list.getY(), sideW, list.getHeight(), true);
			addRenderableWidget(tabs);

			final int featuresY = 0; // top
			final int tweaksY = list.getChildScroll(indexTweaks);
			final int debugY = list.getChildScroll(indexDebug);

			tabs.addItem(new ActionItem("config.canvas.category.features",
					Buttons.SidebarButton::new, () -> list.setScrollAmount(featuresY)));

			tabs.addItem(new ActionItem("config.canvas.category.tweaks",
					Buttons.SidebarButton::new, () -> list.setScrollAmount(tweaksY)));

			tabs.addItem(new ActionItem("config.canvas.category.debug",
					Buttons.SidebarButton::new, () -> list.setScrollAmount(debugY)));
		}

		final var saveButton = this.addRenderableWidget(new Button(this.width / 2 + 1, this.height - 35 + 6, 120 - 2, 20, CommonComponents.GUI_DONE, b -> save()));
		this.addRenderableWidget(new Button(this.width / 2 - 120 - 1, this.height - 35 + 6, 120 - 2, 20, CommonComponents.GUI_CANCEL, b -> onClose()));

		optionSession.setSaveButton(saveButton);
	}

	private void save() {
		// pipeline changes aren't handled here
		editing.pipelineId = Configurator.pipelineId;

		Configurator.readFromConfig(editing);

		// for now Config reload does reload everything including Timekeeper
		if (reloadTimekeeper && !reload) {
			Timekeeper.configOrPipelineReload();
		}

		ConfigManager.saveUserInput(reload ? RELOAD_EVERYTHING : DONT_RELOAD);

		if (requiresRestart) {
			this.minecraft.setScreen(new ConfigRestartScreen(this.parent));
		} else {
			onClose();
		}
	}

	@Override
	protected void renderTooltips(PoseStack poseStack, int i, int j) {
		if (list != null) {
			final List<FormattedCharSequence> tooltip = list.getTooltip(i, j);

			if (tooltip != null) {
				// final int x = list.getRowLeft();
				// final int y = list.getRowBottom(list.getChildIndexAt(i, j)) + 10;
				renderTooltip(poseStack, tooltip, i, j + 30);
			}
		}
	}
}
