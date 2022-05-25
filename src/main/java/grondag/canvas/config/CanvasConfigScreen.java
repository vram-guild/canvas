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

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.option.SpruceSimpleActionOption;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.config.builder.Buttons;
import grondag.canvas.config.builder.Categories;
import grondag.canvas.config.builder.OptionSession;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.terrain.occlusion.TerrainIterator;

public class CanvasConfigScreen extends SpruceScreen {
	private boolean reload;
	private boolean reloadTimekeeper;
	private boolean requiresRestart;

	private final Screen parent;
	private final ConfigData editing;

	public CanvasConfigScreen(Screen parent) {
		super(new TranslatableComponent("config.canvas.title"));

		reload = false;
		reloadTimekeeper = false;
		requiresRestart = false;

		editing = new ConfigData();
		Configurator.writeToConfig(editing);

		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		OptionSession optionSession = new OptionSession();

		Buttons.sideW = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;
		final int rightSideW = Math.min(Buttons.sideW, Math.max(0, this.width - 330 - Buttons.sideW));

		final SpruceOptionListWidget list = new SpruceOptionListWidget(Position.of(Buttons.sideW + 2, 22 + 2), this.width - Buttons.sideW - 4 - rightSideW, this.height - 35 - 22 - 2);
		list.setBackground(new SimpleColorBackground(0xAA000000));
		addWidget(list);

		list.addSingleOptionEntry(new SpruceSimpleActionOption("config.canvas.value.pipeline_config", Buttons::browseButton, e -> {
			minecraft.setScreen(new PipelineOptionScreen(this, new ResourceLocation(Configurator.pipelineId)));
		}));

		// FEATURES
		final int indexFeatures = Categories.addTo(list, "config.canvas.category.features");

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.blend_fluid_colors",
				() -> editing.blendFluidColors,
				b -> {
					reload |= Configurator.blendFluidColors != b;
					editing.blendFluidColors = b;
				},
				DEFAULTS.blendFluidColors,
				ConfigManager.parseTooltip("config.canvas.help.blend_fluid_colors")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.wavy_grass",
				() -> editing.wavyGrass,
				b -> {
					reload |= Configurator.wavyGrass != b;
					editing.wavyGrass = b;
				},
				DEFAULTS.wavyGrass,
				ConfigManager.parseTooltip("config.canvas.help.wavy_grass")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.disable_vignette",
				() -> editing.disableVignette,
				b -> editing.disableVignette = b,
				DEFAULTS.disableVignette,
				ConfigManager.parseTooltip("config.canvas.help.disable_vignette")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.semi_flat_lighting",
				() -> editing.semiFlatLighting,
				b -> {
					reload |= Configurator.semiFlatLighting != b;
					editing.semiFlatLighting = b;
				},
				DEFAULTS.semiFlatLighting,
				ConfigManager.parseTooltip("config.canvas.help.semi_flat_lighting")).spruceOption());

		// TWEAKS
		final int indexTweaks = Categories.addTo(list, "config.canvas.category.tweaks");

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.adjust_vanilla_geometry",
				() -> editing.preventDepthFighting,
				b -> {
					reload |= Configurator.preventDepthFighting != b;
					editing.preventDepthFighting = b;
				},
				DEFAULTS.preventDepthFighting,
				ConfigManager.parseTooltip("config.canvas.help.adjust_vanilla_geometry")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.clamp_exterior_vertices",
				() -> editing.clampExteriorVertices,
				b -> {
					reload |= Configurator.clampExteriorVertices != b;
					editing.clampExteriorVertices = b;
				},
				DEFAULTS.clampExteriorVertices,
				ConfigManager.parseTooltip("config.canvas.help.clamp_exterior_vertices")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.fix_luminous_block_shade",
				() -> editing.fixLuminousBlockShading,
				b -> {
					reload |= Configurator.fixLuminousBlockShading != b;
					editing.fixLuminousBlockShading = b;
				},
				DEFAULTS.fixLuminousBlockShading,
				ConfigManager.parseTooltip("config.canvas.help.fix_luminous_block_shade")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.advanced_terrain_culling",
				() -> editing.advancedTerrainCulling,
				b -> {
					reload |= Configurator.advancedTerrainCulling != b;
					editing.advancedTerrainCulling = b;
				},
				DEFAULTS.advancedTerrainCulling,
				ConfigManager.parseTooltip("config.canvas.help.advanced_terrain_culling")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.terrain_setup_off_thread",
				() -> editing.terrainSetupOffThread,
				b -> {
					reload |= Configurator.terrainSetupOffThread != b;
					editing.terrainSetupOffThread = b;
				},
				DEFAULTS.terrainSetupOffThread,
				ConfigManager.parseTooltip("config.canvas.help.terrain_setup_off_thread")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.safe_native_allocation",
				() -> editing.safeNativeMemoryAllocation,
				b -> {
					requiresRestart |= Configurator.safeNativeMemoryAllocation != b;
					editing.safeNativeMemoryAllocation = b;
				},
				DEFAULTS.safeNativeMemoryAllocation,
				ConfigManager.parseTooltip("config.canvas.help.safe_native_allocation")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.cull_entity_render",
				() -> editing.cullEntityRender,
				b -> editing.cullEntityRender = b,
				DEFAULTS.cullEntityRender,
				ConfigManager.parseTooltip("config.canvas.help.cull_entity_render")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.greedy_render_thread",
				() -> editing.greedyRenderThread,
				b -> editing.greedyRenderThread = b,
				DEFAULTS.greedyRenderThread,
				ConfigManager.parseTooltip("config.canvas.help.greedy_render_thread")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.force_jmx_loading",
				() -> editing.forceJmxModelLoading,
				b -> editing.forceJmxModelLoading = b,
				DEFAULTS.forceJmxModelLoading,
				ConfigManager.parseTooltip("config.canvas.help.force_jmx_loading")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.reduce_resolution_on_mac",
				() -> editing.reduceResolutionOnMac,
				b -> {
					requiresRestart |= Configurator.reduceResolutionOnMac != b;
					editing.reduceResolutionOnMac = b;
				},
				DEFAULTS.reduceResolutionOnMac,
				ConfigManager.parseTooltip("config.canvas.help.reduce_resolution_on_mac")).spruceOption());

		list.addSingleOptionEntry(optionSession.intOption("config.canvas.value.static_frustum_padding",
				0,
				20,
				1,
				() -> editing.staticFrustumPadding,
				i -> editing.staticFrustumPadding = i,
				DEFAULTS.staticFrustumPadding,
				ConfigManager.parseTooltip("config.canvas.help.static_frustum_padding")).spruceOption());

		list.addSingleOptionEntry(optionSession.intOption("config.canvas.value.dynamic_frustum_padding",
				0,
				30,
				1,
				() -> editing.dynamicFrustumPadding,
				i -> editing.dynamicFrustumPadding = i,
				DEFAULTS.dynamicFrustumPadding,
				ConfigManager.parseTooltip("config.canvas.help.dynamic_frustum_padding")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.cull_particles",
				() -> editing.cullParticles,
				b -> editing.cullParticles = b,
				DEFAULTS.cullParticles,
				ConfigManager.parseTooltip("config.canvas.help.cull_particles")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.enable_near_occluders",
				() -> editing.enableNearOccluders,
				b -> editing.enableNearOccluders = b,
				DEFAULTS.enableNearOccluders,
				ConfigManager.parseTooltip("config.canvas.help.enable_near_occluders")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.use_combined_thread_pool",
				() -> editing.useCombinedThreadPool,
				b -> {
					requiresRestart |= Configurator.useCombinedThreadPool != b;
					editing.useCombinedThreadPool = b;
				},
				DEFAULTS.useCombinedThreadPool,
				ConfigManager.parseTooltip("config.canvas.help.use_combined_thread_pool")).spruceOption());

		list.addSingleOptionEntry(optionSession.enumOption("config.canvas.value.transfer_buffer_mode",
				() -> editing.transferBufferMode,
				e -> {
					reload |= Configurator.transferBufferMode != e;
					editing.transferBufferMode = e;
				},
				DEFAULTS.transferBufferMode,
				TransferBuffers.Config.class,
				ConfigManager.parseTooltip("config.canvas.help.transfer_buffer_mode")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.steady_debug_screen",
				() -> editing.steadyDebugScreen,
				b -> editing.steadyDebugScreen = b,
				DEFAULTS.steadyDebugScreen,
				ConfigManager.parseTooltip("config.canvas.help.steady_debug_screen")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.disable_unseen_sprite_animation",
				() -> editing.disableUnseenSpriteAnimation,
				b -> {
					reload |= Configurator.disableUnseenSpriteAnimation != b;
					editing.disableUnseenSpriteAnimation = b;
				},
				DEFAULTS.disableUnseenSpriteAnimation,
				ConfigManager.parseTooltip("config.canvas.help.disable_unseen_sprite_animation")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.group_animated_sprites",
				() -> editing.groupAnimatedSprites,
				b -> editing.groupAnimatedSprites = b,
				DEFAULTS.groupAnimatedSprites,
				ConfigManager.parseTooltip("config.canvas.help.group_animated_sprites")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.cull_backfacing_terrain",
				() -> editing.cullBackfacingTerrain,
				b -> {
					reload |= Configurator.cullBackfacingTerrain != b;
					editing.cullBackfacingTerrain = b;
				},
				DEFAULTS.cullBackfacingTerrain,
				ConfigManager.parseTooltip("config.canvas.help.cull_backfacing_terrain")).spruceOption());

		// DEBUG
		final int indexDebug = Categories.addTo(list, "config.canvas.category.debug");

		list.addSingleOptionEntry(optionSession.enumOption("config.canvas.value.shadow_priming_strategy",
				() -> editing.shadowPrimingStrategy,
				e -> {
					reload |= Configurator.shadowPrimingStrategy != e;
					editing.shadowPrimingStrategy = e;
				},
				DEFAULTS.shadowPrimingStrategy,
				TerrainIterator.ShadowPriming.class,
				ConfigManager.parseTooltip("config.canvas.help.shadow_priming_strategy")).spruceOption());

		list.addSingleOptionEntry(optionSession.intOption("config.canvas.value.shadow_max_distance",
				4,
				32,
				1,
				() -> editing.shadowMaxDistance,
				i -> editing.shadowMaxDistance = i,
				DEFAULTS.shadowMaxDistance,
				ConfigManager.parseTooltip("config.canvas.help.shadow_max_distance")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.shader_debug",
				() -> editing.shaderDebug,
				b -> editing.shaderDebug = b,
				DEFAULTS.shaderDebug,
				ConfigManager.parseTooltip("config.canvas.help.shader_debug")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.preprocess_shader_source",
				() -> editing.preprocessShaderSource,
				b -> {
					reload |= Configurator.preprocessShaderSource != b;
					editing.preprocessShaderSource = b;
				},
				DEFAULTS.preprocessShaderSource,
				ConfigManager.parseTooltip("config.canvas.help.preprocess_shader_source")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.concise_errors",
				() -> editing.conciseErrors,
				b -> editing.conciseErrors = b,
				DEFAULTS.conciseErrors,
				ConfigManager.parseTooltip("config.canvas.help.concise_errors")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.log_machine_info",
				() -> editing.logMachineInfo,
				b -> editing.logMachineInfo = b,
				DEFAULTS.logMachineInfo,
				ConfigManager.parseTooltip("config.canvas.help.log_machine_info")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.log_gl_state_changes",
				() -> editing.logGlStateChanges,
				b -> editing.logGlStateChanges = b,
				DEFAULTS.logGlStateChanges,
				ConfigManager.parseTooltip("config.canvas.help.log_gl_state_changes")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.debug_native_allocation",
				() -> editing.debugNativeMemoryAllocation,
				b -> {
					requiresRestart |= Configurator.debugNativeMemoryAllocation != b;
					editing.debugNativeMemoryAllocation = b;
				},
				DEFAULTS.debugNativeMemoryAllocation,
				ConfigManager.parseTooltip("config.canvas.help.debug_native_allocation")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.debug_occlusion_raster",
				() -> editing.debugOcclusionRaster,
				b -> editing.debugOcclusionRaster = b,
				DEFAULTS.debugOcclusionRaster,
				ConfigManager.parseTooltip("config.canvas.help.debug_occlusion_raster")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.debug_occlusion_boxes",
				() -> editing.debugOcclusionBoxes,
				b -> editing.debugOcclusionBoxes = b,
				DEFAULTS.debugOcclusionBoxes,
				ConfigManager.parseTooltip("config.canvas.help.debug_occlusion_boxes")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.white_glass_occludes_terrain",
				() -> editing.renderWhiteGlassAsOccluder,
				b -> {
					reload |= Configurator.renderWhiteGlassAsOccluder != b;
					editing.renderWhiteGlassAsOccluder = b;
				},
				DEFAULTS.renderWhiteGlassAsOccluder,
				ConfigManager.parseTooltip("config.canvas.help.white_glass_occludes_terrain")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.trace_occlusion_edge_cases",
				() -> editing.traceOcclusionEdgeCases,
				b -> editing.traceOcclusionEdgeCases = b,
				DEFAULTS.traceOcclusionEdgeCases,
				ConfigManager.parseTooltip("config.canvas.help.trace_occlusion_edge_cases")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.buffer_debug",
				() -> editing.enableBufferDebug,
				b -> editing.enableBufferDebug = b,
				DEFAULTS.enableBufferDebug,
				ConfigManager.parseTooltip("config.canvas.help.buffer_debug")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.lifecycle_debug",
				() -> editing.enableLifeCycleDebug,
				b -> editing.enableLifeCycleDebug = b,
				DEFAULTS.enableLifeCycleDebug,
				ConfigManager.parseTooltip("config.canvas.help.lifecycle_debug")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.log_missing_uniforms",
				() -> editing.logMissingUniforms,
				b -> editing.logMissingUniforms = b,
				DEFAULTS.logMissingUniforms,
				ConfigManager.parseTooltip("config.canvas.help.log_missing_uniforms")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.log_materials",
				() -> editing.logMaterials,
				b -> editing.logMaterials = b,
				DEFAULTS.logMaterials,
				ConfigManager.parseTooltip("config.canvas.help.log_materials")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.log_render_lag_spikes",
				() -> editing.logRenderLagSpikes,
				b -> {
					reloadTimekeeper |= Configurator.logRenderLagSpikes != b;
					editing.logRenderLagSpikes = b;
				},
				DEFAULTS.logRenderLagSpikes,
				ConfigManager.parseTooltip("config.canvas.help.log_render_lag_spikes")).spruceOption());

		list.addSingleOptionEntry(optionSession.intOption("config.canvas.value.render_lag_spike_fps",
				30,
				120,
				1,
				() -> editing.renderLagSpikeFps,
				i -> editing.renderLagSpikeFps = i,
				DEFAULTS.renderLagSpikeFps,
				ConfigManager.parseTooltip("config.canvas.help.render_lag_spike_fps")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.display_render_profiler",
				() -> editing.displayRenderProfiler,
				b -> {
					reloadTimekeeper |= Configurator.displayRenderProfiler != b;
					editing.displayRenderProfiler = b;
				},
				DEFAULTS.displayRenderProfiler,
				ConfigManager.parseTooltip("config.canvas.help.display_render_profiler")).spruceOption());

		list.addSingleOptionEntry(optionSession.enumOption("config.canvas.value.profiler_display_mode",
				() -> editing.profilerDisplayMode,
				e -> {
					reloadTimekeeper |= Configurator.profilerDisplayMode != e;
					editing.profilerDisplayMode = e;
				},
				DEFAULTS.profilerDisplayMode,
				Timekeeper.Mode.class,
				ConfigManager.parseTooltip("config.canvas.help.profiler_display_mode")).spruceOption());

		list.addSingleOptionEntry(optionSession.intOption("config.canvas.value.profiler_detail_level",
				0,
				2,
				1,
				() -> editing.profilerDetailLevel,
				i -> editing.profilerDetailLevel = i,
				DEFAULTS.profilerDetailLevel,
				ConfigManager.parseTooltip("config.canvas.help.profiler_detail_level")).spruceOption());

		list.addSingleOptionEntry(optionSession.floatOption("config.canvas.value.profiler_overlay_scale",
				0.0f,
				1.0f,
				0.1f,
				() -> editing.profilerOverlayScale,
				f -> editing.profilerOverlayScale = f,
				DEFAULTS.profilerOverlayScale,
				ConfigManager.parseTooltip("config.canvas.help.profiler_overlay_scale")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.debug_sprite_atlas",
				() -> editing.debugSpriteAtlas,
				b -> editing.debugSpriteAtlas = b,
				DEFAULTS.debugSpriteAtlas,
				ConfigManager.parseTooltip("config.canvas.help.debug_sprite_atlas")).spruceOption());

		list.addSingleOptionEntry(optionSession.booleanOption("config.canvas.value.trace_texture_load",
				() -> editing.traceTextureLoad,
				b -> editing.traceTextureLoad = b,
				DEFAULTS.traceTextureLoad,
				ConfigManager.parseTooltip("config.canvas.help.trace_texture_load")).spruceOption());

		if (Buttons.sideW > 0) {
			final SpruceOptionListWidget tabs = new SpruceOptionListWidget(Position.of(1, list.getY()), Buttons.sideW, list.getHeight());
			tabs.setBackground(EmptyBackground.EMPTY_BACKGROUND);
			addWidget(tabs);

			final int featuresY = 0; // top
			final int tweaksY = list.children().get(indexTweaks).getY() - list.getY() - 2;
			final int debugY = list.children().get(indexDebug).getY() - list.getY() - 2;

			tabs.addSingleOptionEntry(new SpruceSimpleActionOption("config.canvas.category.features",
					Buttons::sideButton, e -> list.setScrollAmount(featuresY)));

			tabs.addSingleOptionEntry(new SpruceSimpleActionOption("config.canvas.category.tweaks",
					Buttons::sideButton, e -> list.setScrollAmount(tweaksY)));

			tabs.addSingleOptionEntry(new SpruceSimpleActionOption("config.canvas.category.debug",
					Buttons::sideButton, e -> list.setScrollAmount(debugY)));
		}

		var saveButton = this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height - 35 + 6), 120 - 2, 20, CommonComponents.GUI_DONE, b -> save()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 120 - 1, this.height - 35 + 6), 120 - 2, 20, CommonComponents.GUI_CANCEL, b -> close()));

		optionSession.setSaveButton(saveButton);
	}

	private void close() {
		this.minecraft.setScreen(this.parent);
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
			close();
		}
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 8, 16777215);
	}
}
