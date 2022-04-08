package grondag.canvas.config;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.SimpleColorBackground;
import dev.lambdaurora.spruceui.option.*;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.config.widget.ConfigSession;
import grondag.canvas.perf.Timekeeper;

import static grondag.canvas.config.ConfigManager.DEFAULTS;

public class CanvasConfigScreen extends ConfigSession {
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

		final int tabsSize = (this.width - 330) >= 72 ? Math.min(120, this.width - 330) : 0;

		final SpruceOptionListWidget list = new SpruceOptionListWidget(Position.of(tabsSize + 2, 22), this.width - tabsSize - 4, this.height - 35 - 22);
		list.setBackground(new SimpleColorBackground(0xAA000000));
		addWidget(list);

		// FEATURES
		final int indexFeatures = list.addSingleOptionEntry(new SpruceSeparatorOption("config.canvas.category.features", true, null));
//		pipeline = new PipelineSelectorEntry(PipelineLoader.get(pipelineId));
//		features.addEntry(pipeline);
//		features.addEntry(new PipelineOptionsEntry());


		list.addSingleOptionEntry(booleanOption("config.canvas.value.blend_fluid_colors",
				() -> editing.blendFluidColors,
				b -> {
					reload |= Configurator.blendFluidColors != b;
					editing.blendFluidColors = b;
				},
				DEFAULTS.blendFluidColors,
				createTooltipComponent("config.canvas.help.blend_fluid_colors")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.wavy_grass",
				() -> editing.wavyGrass,
				b -> {
					reload |= Configurator.wavyGrass != b;
					editing.wavyGrass = b;
				},
				DEFAULTS.wavyGrass,
				createTooltipComponent("config.canvas.help.wavy_grass")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.disable_vignette",
				() -> editing.disableVignette,
				b -> editing.disableVignette = b,
				DEFAULTS.disableVignette,
				createTooltipComponent("config.canvas.help.disable_vignette")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.semi_flat_lighting",
				() -> editing.semiFlatLighting,
				b -> editing.semiFlatLighting = b,
				DEFAULTS.semiFlatLighting,
				createTooltipComponent("config.canvas.help.semi_flat_lighting")));

		// TWEAKS
		final int indexTweaks = list.addSingleOptionEntry(new SpruceSeparatorOption("config.canvas.category.tweaks", true, null));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.adjust_vanilla_geometry",
				() -> editing.preventDepthFighting,
				b -> editing.preventDepthFighting = b,
				DEFAULTS.preventDepthFighting,
				createTooltipComponent("config.canvas.help.adjust_vanilla_geometry")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.clamp_exterior_vertices",
				() -> editing.clampExteriorVertices,
				b -> {
					reload |= Configurator.clampExteriorVertices != b;
					editing.clampExteriorVertices = b;
				},
				DEFAULTS.clampExteriorVertices,
				new TranslatableComponent("config.canvas.help.clamp_exterior_vertices")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.fix_luminous_block_shade",
				() -> editing.fixLuminousBlockShading,
				b -> {
					reload |= Configurator.fixLuminousBlockShading != b;
					editing.fixLuminousBlockShading = b;
				},
				DEFAULTS.fixLuminousBlockShading,
				createTooltipComponent("config.canvas.help.fix_luminous_block_shade")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.advanced_terrain_culling",
				() -> editing.advancedTerrainCulling,
				b -> {
					reload |= Configurator.advancedTerrainCulling != b;
					editing.advancedTerrainCulling = b;
				},
				DEFAULTS.advancedTerrainCulling,
				createTooltipComponent("config.canvas.help.advanced_terrain_culling")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.terrain_setup_off_thread",
				() -> editing.terrainSetupOffThread,
				b -> {
					reload |= Configurator.terrainSetupOffThread != b;
					editing.terrainSetupOffThread = b;
				},
				DEFAULTS.terrainSetupOffThread,
				createTooltipComponent("config.canvas.help.terrain_setup_off_thread")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.safe_native_allocation",
				() -> editing.safeNativeMemoryAllocation,
				b -> editing.safeNativeMemoryAllocation = b,
				DEFAULTS.safeNativeMemoryAllocation,
				createTooltipComponent("config.canvas.help.safe_native_allocation")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.cull_entity_render",
				() -> editing.cullEntityRender,
				b -> editing.cullEntityRender = b,
				DEFAULTS.cullEntityRender,
				createTooltipComponent("config.canvas.help.cull_entity_render")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.greedy_render_thread",
				() -> editing.greedyRenderThread,
				b -> editing.greedyRenderThread = b,
				DEFAULTS.greedyRenderThread,
				createTooltipComponent("config.canvas.help.greedy_render_thread")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.force_jmx_loading",
				() -> editing.forceJmxModelLoading,
				b -> editing.forceJmxModelLoading = b,
				DEFAULTS.forceJmxModelLoading,
				createTooltipComponent("config.canvas.help.force_jmx_loading")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.reduce_resolution_on_mac",
				() -> editing.reduceResolutionOnMac,
				b -> {
					requiresRestart |= Configurator.reduceResolutionOnMac != b;
					editing.reduceResolutionOnMac = b;
				},
				DEFAULTS.reduceResolutionOnMac,
				createTooltipComponent("config.canvas.help.reduce_resolution_on_mac")));

		list.addSingleOptionEntry(intOption("config.canvas.value.static_frustum_padding",
				0,
				20,
				1,
				() -> editing.staticFrustumPadding,
				i -> editing.staticFrustumPadding = i,
				DEFAULTS.staticFrustumPadding,
				createTooltipComponent("config.canvas.help.static_frustum_padding")));

		list.addSingleOptionEntry(intOption("config.canvas.value.dynamic_frustum_padding",
				0,
				30,
				1,
				() -> editing.dynamicFrustumPadding,
				i -> editing.dynamicFrustumPadding = i,
				DEFAULTS.dynamicFrustumPadding,
				createTooltipComponent("config.canvas.help.dynamic_frustum_padding")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.cull_particles",
				() -> editing.cullParticles,
				b -> editing.cullParticles = b,
				DEFAULTS.cullParticles,
				createTooltipComponent("config.canvas.help.cull_particles")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.enable_near_occluders",
				() -> editing.enableNearOccluders,
				b -> editing.enableNearOccluders = b,
				DEFAULTS.enableNearOccluders,
				createTooltipComponent("config.canvas.help.enable_near_occluders")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.use_combined_thread_pool",
				() -> editing.useCombinedThreadPool,
				b -> {
					requiresRestart |= Configurator.useCombinedThreadPool != b;
					editing.useCombinedThreadPool = b;
				},
				DEFAULTS.useCombinedThreadPool,
				createTooltipComponent("config.canvas.help.use_combined_thread_pool")));

		list.addSingleOptionEntry(enumOption("config.canvas.value.transfer_buffer_mode",
				() -> editing.transferBufferMode,
				e -> editing.transferBufferMode = e,
				DEFAULTS.transferBufferMode,
				TransferBuffers.Config.class,
				createTooltipComponent("config.canvas.help.transfer_buffer_mode")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.steady_debug_screen",
				() -> editing.steadyDebugScreen,
				b -> editing.steadyDebugScreen = b,
				DEFAULTS.steadyDebugScreen,
				createTooltipComponent("config.canvas.help.steady_debug_screen")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.disable_unseen_sprite_animation",
				() -> editing.disableUnseenSpriteAnimation,
				b -> {
					reload |= Configurator.disableUnseenSpriteAnimation != b;
					editing.disableUnseenSpriteAnimation = b;
				},
				DEFAULTS.disableUnseenSpriteAnimation,
				createTooltipComponent("config.canvas.help.dis,able_unseen_sprite_animation")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.group_animated_sprites",
				() -> editing.groupAnimatedSprites,
				b -> editing.groupAnimatedSprites = b,
				DEFAULTS.groupAnimatedSprites,
				createTooltipComponent("config.canvas.help.group_animated_sprites")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.cull_backfacing_terrain",
				() -> editing.cullBackfacingTerrain,
				b -> {
					reload |= Configurator.cullBackfacingTerrain != b;
					editing.cullBackfacingTerrain = b;
				},
				DEFAULTS.cullBackfacingTerrain,
				createTooltipComponent("config.canvas.help.cull_backfacing_terrain")));

		// DEBUG
		final int indexDebug = list.addSingleOptionEntry(new SpruceSeparatorOption("config.canvas.category.debug", true, null));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.shader_debug",
				() -> editing.shaderDebug,
				b -> editing.shaderDebug = b,
				DEFAULTS.shaderDebug,
				createTooltipComponent("config.canvas.help.shader_debug")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.preprocess_shader_source",
				() -> editing.preprocessShaderSource,
				b -> {
					reload |= Configurator.preprocessShaderSource != b;
					editing.preprocessShaderSource = b;
				},
				DEFAULTS.preprocessShaderSource,
				createTooltipComponent("config.canvas.help.preprocess_shader_source")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.concise_errors",
				() -> editing.conciseErrors,
				b -> editing.conciseErrors = b,
				DEFAULTS.conciseErrors,
				createTooltipComponent("config.canvas.help.concise_errors")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.log_machine_info",
				() -> editing.logMachineInfo,
				b -> editing.logMachineInfo = b,
				DEFAULTS.logMachineInfo,
				createTooltipComponent("config.canvas.help.log_machine_info")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.log_gl_state_changes",
				() -> editing.logGlStateChanges,
				b -> editing.logGlStateChanges = b,
				DEFAULTS.logGlStateChanges,
				createTooltipComponent("config.canvas.help.log_gl_state_changes")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.debug_native_allocation",
				() -> editing.debugNativeMemoryAllocation,
				b -> editing.debugNativeMemoryAllocation = b,
				DEFAULTS.debugNativeMemoryAllocation,
				createTooltipComponent("config.canvas.help.debug_native_allocation")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.debug_occlusion_raster",
				() -> editing.debugOcclusionRaster,
				b -> editing.debugOcclusionRaster = b,
				DEFAULTS.debugOcclusionRaster,
				createTooltipComponent("config.canvas.help.debug_occlusion_raster")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.debug_occlusion_boxes",
				() -> editing.debugOcclusionBoxes,
				b -> editing.debugOcclusionBoxes = b,
				DEFAULTS.debugOcclusionBoxes,
				createTooltipComponent("config.canvas.help.debug_occlusion_boxes")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.white_glass_occludes_terrain",
				() -> editing.renderWhiteGlassAsOccluder,
				b -> {
					reload |= Configurator.renderWhiteGlassAsOccluder != b;
					editing.renderWhiteGlassAsOccluder = b;
				},
				DEFAULTS.renderWhiteGlassAsOccluder,
				createTooltipComponent("config.canvas.help.white_glass_occludes_terrain")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.trace_occlusion_edge_cases",
				() -> editing.traceOcclusionEdgeCases,
				b -> editing.traceOcclusionEdgeCases = b,
				DEFAULTS.traceOcclusionEdgeCases,
				createTooltipComponent("config.canvas.help.trace_occlusion_edge_cases")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.buffer_debug",
				() -> editing.enableBufferDebug,
				b -> editing.enableBufferDebug = b,
				DEFAULTS.enableBufferDebug,
				createTooltipComponent("config.canvas.help.buffer_debug")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.lifecycle_debug",
				() -> editing.enableLifeCycleDebug,
				b -> editing.enableLifeCycleDebug = b,
				DEFAULTS.enableLifeCycleDebug,
				createTooltipComponent("config.canvas.help.lifecycle_debug")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.log_missing_uniforms",
				() -> editing.logMissingUniforms,
				b -> editing.logMissingUniforms = b,
				DEFAULTS.logMissingUniforms,
				createTooltipComponent("config.canvas.help.log_missing_uniforms")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.log_materials",
				() -> editing.logMaterials,
				b -> editing.logMaterials = b,
				DEFAULTS.logMaterials,
				createTooltipComponent("config.canvas.help.log_materials")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.log_render_lag_spikes",
				() -> editing.logRenderLagSpikes,
				b -> {
					reloadTimekeeper |= Configurator.logRenderLagSpikes != b;
					editing.logRenderLagSpikes = b;
				},
				DEFAULTS.logRenderLagSpikes,
				createTooltipComponent("config.canvas.help.log_render_lag_spikes")));

		list.addSingleOptionEntry(intOption("config.canvas.value.render_lag_spike_fps",
				30,
				120,
				1,
				() -> editing.renderLagSpikeFps,
				i -> editing.renderLagSpikeFps = i,
				DEFAULTS.renderLagSpikeFps,
				createTooltipComponent("config.canvas.help.render_lag_spike_fps")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.display_render_profiler",
				() -> editing.displayRenderProfiler,
				b -> {
					reloadTimekeeper |= Configurator.displayRenderProfiler != b;
					editing.displayRenderProfiler = b;
				},
				DEFAULTS.displayRenderProfiler,
				createTooltipComponent("config.canvas.help.display_render_profiler")));

		list.addSingleOptionEntry(enumOption("config.canvas.value.profiler_display_mode",
				() -> editing.profilerDisplayMode,
				e -> {
					reloadTimekeeper |= Configurator.profilerDisplayMode != e;
					editing.profilerDisplayMode = e;
				},
				DEFAULTS.profilerDisplayMode,
				Timekeeper.Mode.class,
				createTooltipComponent("config.canvas.help.profiler_display_mode")));

		list.addSingleOptionEntry(intOption("config.canvas.value.profiler_detail_level",
				0,
				2,
				1,
				() -> editing.profilerDetailLevel,
				i -> editing.profilerDetailLevel = i,
				DEFAULTS.profilerDetailLevel,
				createTooltipComponent("config.canvas.help.profiler_detail_level")));

		list.addSingleOptionEntry(floatOption("config.canvas.value.profiler_overlay_scale",
				0.0f,
				1.0f,
				0.1f,
				() -> editing.profilerOverlayScale,
				f -> editing.profilerOverlayScale = f,
				DEFAULTS.profilerOverlayScale,
				createTooltipComponent("config.canvas.help.profiler_overlay_scale")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.debug_sprite_atlas",
				() -> editing.debugSpriteAtlas,
				b -> editing.debugSpriteAtlas = b,
				DEFAULTS.debugSpriteAtlas,
				createTooltipComponent("config.canvas.help.debug_sprite_atlas")));

		list.addSingleOptionEntry(booleanOption("config.canvas.value.trace_texture_load",
				() -> editing.traceTextureLoad,
				b -> editing.traceTextureLoad = b,
				DEFAULTS.traceTextureLoad,
				createTooltipComponent("config.canvas.help.trace_texture_load")));

		if (tabsSize > 0) {
			final SpruceContainerWidget tabs = new SpruceContainerWidget(Position.of(1, list.getY()), tabsSize, this.height - 35 - 22);

			addWidget(tabs);

			final int featuresY = list.children().get(indexFeatures).getY() - list.getY() - 2;
			final int tweaksY = list.children().get(indexTweaks).getY() - list.getY() - 2;
			final int debugY = list.children().get(indexDebug).getY() - list.getY() - 2;

			tabs.addChild(new SpruceButtonWidget(Position.of(1, 1 + 21 * 0), tabs.getWidth() - 2, 20, new TranslatableComponent("config.canvas.category.features"), button -> list.setScrollAmount(featuresY)));
			tabs.addChild(new SpruceButtonWidget(Position.of(1, 1 + 21 * 1), tabs.getWidth() - 2, 20, new TranslatableComponent("config.canvas.category.tweaks"), button -> list.setScrollAmount(tweaksY)));
			tabs.addChild(new SpruceButtonWidget(Position.of(1, 1 + 21 * 2), tabs.getWidth() - 2, 20, new TranslatableComponent("config.canvas.category.debug"), button -> list.setScrollAmount(debugY)));
		}

		// TO-DO Translatable
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 160 - 1, this.height - 35 + 6), 160 - 2, 20, new TextComponent("Save & Quit"), b -> save()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height - 35 + 6), 160 - 2, 20, new TextComponent("Cancel"), b -> cancel()));
	}

	private void closeWindow() {
		this.minecraft.setScreen(this.parent);
	}

	private void save() {
		close();

		Configurator.readFromConfig(editing);
		ConfigManager.saveConfig();

		if (reload) {
			Configurator.reload = true;
		}

		if (reloadTimekeeper) {
			Timekeeper.configOrPipelineReload();
		}

		if (requiresRestart) {
			this.minecraft.setScreen(new ConfigRestartScreen(this.parent));
		} else {
			closeWindow();
		}
	}

	private void cancel() {
		close();
		closeWindow();
	}

	private static Component createTooltipComponent(String key) {
		String translated = I18n.get(key);
		translated = translated.replace(";", " ");
		translated = translated.replace("  ", " ");
		return new TextComponent(translated);
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 8, 16777215);
	}
}
