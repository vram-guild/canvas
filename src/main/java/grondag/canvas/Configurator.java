/*******************************************************************************
 * Copyright 2019 grondag
 * 
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
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import grondag.canvas.material.ShaderManager;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.cloth.api.ConfigScreenBuilder;
import me.shedaniel.cloth.api.ConfigScreenBuilder.SavedConfig;
import me.shedaniel.cloth.gui.entries.BooleanListEntry;
import me.shedaniel.cloth.gui.entries.EnumListEntry;
import me.shedaniel.cloth.gui.entries.IntegerSliderEntry;
import me.shedaniel.cloth.gui.entries.LongListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class Configurator implements ModMenuApi {
    
    @SuppressWarnings("hiding")
    static class ConfigData {
        //TODO: remove
        @Comment("Increase if 'Max shader material exceeded' error occurs. Larger values consume a small amount of memory.")
        int maxPipelines = 128;
        
        @Comment("Applies material properties and shaders to items. (WIP)")
        boolean enableItemRender = false;
        
        @Comment("TODO")
        boolean hardcoreDarkness = false;
        
        @Comment("TODO")
        boolean subtleFog = false;
        
        //TODO: docs
        @Comment("TODO")
        int maxLightmapDelayFrames = 0;
        
        @Comment("TODO")
        long minChunkBudgetNanos = 100000;
        
        @Comment("TODO")
        boolean enableCompactGPUFormats = false;
        
        @Comment("TODO")
        boolean enableHdLightmaps = false;
        
        @Comment("TODO")
        boolean enableLightmapNoise = false;
        
        @Comment("TODO")
        DiffuseMode diffuseShadingMode = DiffuseMode.NORMAL;
        
        @Comment("TODO")
        boolean enableLightSmoothing = false;
        
        @Comment("TODO")
        AoMode aoShadingMode = AoMode.NORMAL;
        
        @Comment("TODO")
        boolean enableSinglePassCutout = true;
        
        @Comment("TODO")
        boolean enableImprovedChunkOcclusion = true;
        
        @Comment("TODO")
        boolean enableBatchedChunkRender = true;
        
        @Comment("TODO")
        boolean disableVanillaChunkMatrix = true;
        
        @Comment("TODO")
        boolean adjustVanillaModelGeometry = true;

        // DEBUG
        @Comment("Output runtime per-material shader source. For shader development debugging.")
        boolean enableShaderDebug = false;
        
        @Comment("TODO")        
        boolean enableLightmapDebug = false;
        
        @Comment("TODO")        
        boolean enableConciseErrors = true;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static boolean enableItemRender = DEFAULTS.enableItemRender;
    public static boolean hardcoreDarkness = DEFAULTS.hardcoreDarkness;
    public static boolean subtleFog = DEFAULTS.subtleFog;
    public static boolean enableShaderDebug = DEFAULTS.enableShaderDebug;
    public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
    
    public static boolean enableHdLightmaps = DEFAULTS.enableHdLightmaps;
    public static boolean enableLightmapNoise = DEFAULTS.enableLightmapNoise;
    public static DiffuseMode diffuseShadingMode = DEFAULTS.diffuseShadingMode;
    public static boolean enableLightSmoothing = DEFAULTS.enableLightSmoothing;
    public static AoMode aoShadingMode = DEFAULTS.aoShadingMode;
    
    public static long minChunkBudgetNanos = DEFAULTS.minChunkBudgetNanos;
    public static boolean enableCompactGPUFormats = DEFAULTS.enableCompactGPUFormats;
    
    public static boolean enableSinglePassCutout = DEFAULTS.enableSinglePassCutout;
    public static boolean enableImprovedChunkOcclusion = DEFAULTS.enableImprovedChunkOcclusion;
    public static boolean enableBatchedChunkRender = DEFAULTS.enableBatchedChunkRender;
    public static boolean disableVanillaChunkMatrix = DEFAULTS.disableVanillaChunkMatrix;
    public static boolean adjustVanillaModelGeometry = DEFAULTS.adjustVanillaModelGeometry;
    public static boolean enableLightmapDebug = DEFAULTS.enableLightmapDebug;
    public static boolean enableConciseErrors = DEFAULTS.enableConciseErrors;

    
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
            JsonObject configJson = JANKSON.load(configFile);
            String regularized = configJson.toJson(false, false, 0);
            config = GSON.fromJson(regularized, ConfigData.class);
        } catch (Exception e) {
            e.printStackTrace();
            CanvasMod.LOG.error("Unable to load config. Using default values.");
        }
        enableItemRender = config.enableItemRender;
        hardcoreDarkness = config.hardcoreDarkness;
        subtleFog = config.subtleFog;
        enableShaderDebug = config.enableShaderDebug;
        enableCompactGPUFormats = config.enableCompactGPUFormats;
        minChunkBudgetNanos = config.minChunkBudgetNanos;
        maxLightmapDelayFrames = config.maxLightmapDelayFrames;
        
        enableHdLightmaps = config.enableHdLightmaps;
        enableLightmapNoise = config.enableLightmapNoise;
        diffuseShadingMode = config.diffuseShadingMode;
        enableLightSmoothing = config.enableLightSmoothing;
        aoShadingMode = config.aoShadingMode;
        
        enableSinglePassCutout = config.enableSinglePassCutout;
        enableImprovedChunkOcclusion = config.enableImprovedChunkOcclusion;
        enableBatchedChunkRender = config.enableBatchedChunkRender;
        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
        adjustVanillaModelGeometry = config.adjustVanillaModelGeometry;
        
        enableLightmapDebug = config.enableLightmapDebug;
        enableConciseErrors = config.enableConciseErrors;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.enableItemRender = enableItemRender;
        config.hardcoreDarkness = hardcoreDarkness;
        config.subtleFog = subtleFog;
        config.enableShaderDebug = enableShaderDebug;
        config.enableCompactGPUFormats = enableCompactGPUFormats;
        config.minChunkBudgetNanos = minChunkBudgetNanos;
        config.maxLightmapDelayFrames = maxLightmapDelayFrames;
        
        config.enableHdLightmaps = enableHdLightmaps;
        config.enableLightmapNoise = enableLightmapNoise;
        config.diffuseShadingMode = diffuseShadingMode;
        config.enableLightSmoothing = enableLightSmoothing;
        config.aoShadingMode = aoShadingMode; 
        
        config.enableSinglePassCutout = enableSinglePassCutout;
        config.enableImprovedChunkOcclusion = enableImprovedChunkOcclusion;
        config.enableBatchedChunkRender = enableBatchedChunkRender;
        config.disableVanillaChunkMatrix = disableVanillaChunkMatrix;
        config.adjustVanillaModelGeometry = adjustVanillaModelGeometry;
        
        config.enableLightmapDebug = enableLightmapDebug;
        config.enableConciseErrors = enableConciseErrors;
        
        try {
            String result = JANKSON.toJson(config).toJson(true, true, 0);
            if (!configFile.exists())
                configFile.createNewFile();
            FileOutputStream out = new FileOutputStream(configFile, false);
            
            out.write(result.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            CanvasMod.LOG.error("Unable to save config.");
            return;
        }
    }
    
    static boolean reloadTerrain = false;
    static boolean reloadShaders = false;
    
    public static enum AoMode {
        NORMAL,
        SUBTLE_ALWAYS,
        SUBTLE_BLOCK_LIGHT,
        NONE;

        @Override
        public String toString() {
            return I18n.translate("config.canvas.enum.ao_mode." + this.name().toLowerCase());
        }
    }
    
    public static enum DiffuseMode {
        NORMAL,
        SKY_ONLY,
        NONE;

        @Override
        public String toString() {
            return I18n.translate("config.canvas.enum.diffuse_mode." + this.name().toLowerCase());
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Screen display() {
        reloadTerrain = false;
        reloadShaders = false;
        
        ConfigScreenBuilder builder = ConfigScreenBuilder.create(screenIn, "config.canvas.title", null);
        
        // FEATURES
        ConfigScreenBuilder.CategoryBuilder features = builder.addCategory("config.canvas.category.features");
        
        features.addOption(new BooleanListEntry("config.canvas.value.item_render", enableItemRender, "config.canvas.reset", 
                () -> DEFAULTS.enableItemRender, b -> enableItemRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        features.addOption(new BooleanListEntry("config.canvas.value.hardcore_darkness", hardcoreDarkness, "config.canvas.reset", 
                () -> DEFAULTS.hardcoreDarkness, b -> {hardcoreDarkness = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hardcore_darkness").split(";"))));
        
        features.addOption(new BooleanListEntry("config.canvas.value.subtle_fog", subtleFog, "config.canvas.reset", 
                () -> DEFAULTS.subtleFog, b -> {subtleFog = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.subtle_fog").split(";"))));
        
        // LIGHTING
        ConfigScreenBuilder.CategoryBuilder lighting = builder.addCategory("config.canvas.category.lighting");
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.light_smoothing", enableLightSmoothing, "config.canvas.reset", 
                () -> DEFAULTS.enableLightSmoothing, b -> {enableLightSmoothing = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.light_smoothing").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.hd_lightmaps", enableHdLightmaps, "config.canvas.reset", 
                () -> DEFAULTS.enableHdLightmaps, b -> {enableHdLightmaps = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hd_lightmaps").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.lightmap_noise", enableLightmapNoise, "config.canvas.reset", 
                () -> DEFAULTS.enableLightmapNoise, b -> {enableLightmapNoise = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_noise").split(";"))));
        
        lighting.addOption(new EnumListEntry(
                "config.canvas.value.diffuse_shading", 
                DiffuseMode.class, 
                diffuseShadingMode, 
                "config.canvas.reset", 
                () -> DEFAULTS.diffuseShadingMode, 
                (b) -> {diffuseShadingMode = (DiffuseMode) b; reloadShaders = true;},
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.canvas.help.diffuse_shading").split(";"))));
        
        lighting.addOption(new EnumListEntry(
                "config.canvas.value.ao_shading", 
                AoMode.class, 
                aoShadingMode, 
                "config.canvas.reset", 
                () -> DEFAULTS.aoShadingMode, 
                (b) -> {aoShadingMode = (AoMode) b; reloadShaders = true;},
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.canvas.help.ao_shading").split(";"))));
        
        lighting.addOption(new IntegerSliderEntry("config.canvas.value.lightmap_delay_frames", 0, 20, maxLightmapDelayFrames, "config.canvas.reset", 
                () -> DEFAULTS.maxLightmapDelayFrames, b -> maxLightmapDelayFrames = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_delay_frames").split(";"))));
        
        // TWEAKS
        ConfigScreenBuilder.CategoryBuilder tweaks = builder.addCategory("config.canvas.category.tweaks");
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.compact_gpu_formats", enableCompactGPUFormats, "config.canvas.reset", 
                () -> DEFAULTS.enableCompactGPUFormats, b -> enableCompactGPUFormats = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.compact_gpu_formats").split(";"))));
        
        tweaks.addOption(new LongListEntry("config.canvas.value.min_chunk_budget", minChunkBudgetNanos, "config.canvas.reset", 
                () -> DEFAULTS.minChunkBudgetNanos, l -> minChunkBudgetNanos = l, 
                () -> Optional.of(I18n.translate("config.canvas.help.min_chunk_budget").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.single_pass_cutout", enableSinglePassCutout, "config.canvas.reset", 
                () -> DEFAULTS.enableSinglePassCutout, b -> enableSinglePassCutout = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.single_pass_cutout").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.chunk_occlusion", enableImprovedChunkOcclusion, "config.canvas.reset", 
                () -> DEFAULTS.enableImprovedChunkOcclusion, b -> enableImprovedChunkOcclusion = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.chunk_occlusion").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.batch_chunk_render", enableBatchedChunkRender, "config.canvas.reset", 
                () -> DEFAULTS.enableBatchedChunkRender, b -> enableBatchedChunkRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.batch_chunk_render").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.vanilla_chunk_matrix", disableVanillaChunkMatrix, "config.canvas.reset", 
                () -> DEFAULTS.disableVanillaChunkMatrix, b -> disableVanillaChunkMatrix = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.vanilla_chunk_matrix").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.adjust_vanilla_geometry", adjustVanillaModelGeometry, "config.canvas.reset", 
                () -> DEFAULTS.adjustVanillaModelGeometry, b -> adjustVanillaModelGeometry = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.adjust_vanilla_geometry").split(";"))));
        
        
        // DEBUG
        ConfigScreenBuilder.CategoryBuilder debug = builder.addCategory("config.canvas.category.debug");
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug", enableShaderDebug, "config.canvas.reset", 
                () -> DEFAULTS.enableShaderDebug, b -> enableShaderDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug_lightmap", enableLightmapDebug, "config.canvas.reset", 
                () -> DEFAULTS.enableLightmapDebug, b -> enableLightmapDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug_lightmap").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.concise_errors", enableConciseErrors, "config.canvas.reset", 
                () -> DEFAULTS.enableConciseErrors, b -> enableConciseErrors = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.concise_errors").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        builder.setOnSave(Configurator::saveUserInput);
        
        return builder.build();
    }
    
    private static void saveUserInput(SavedConfig config) {
        saveConfig();
        
        if(reloadTerrain) {
            MinecraftClient.getInstance().worldRenderer.reload();
        } else if(reloadShaders) {
            ShaderManager.INSTANCE.forceReload();
        }
    }
    
    @Override
    public Optional<Supplier<Screen>> getConfigScreen(Screen screen) {
        screenIn = screen;
        return Optional.of(Configurator::display);
    }
    
    @Override
    public String getModId() {
        return "canvas";
    }
    
    // LEGACY STUFF
    
//    @LangKey("config.acuity_enable_vao")
//    @Comment({"Use Vertex Array Objects if available.",
//        " VAOs generally improve performance when they are supported."})
    public static boolean enable_vao = true;

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
