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
        @Comment("Applies material properties and shaders to items. (WIP)")
        boolean itemShaderRender = false;
        
        @Comment("TODO")
        boolean hardcoreDarkness = false;
        
        @Comment("TODO")
        boolean subtleFog = false;
        
        //TODO: docs
        @Comment("TODO")
        int maxLightmapDelayFrames = 0;
        
        @Comment("TODO")
        long minChunkBudgetNanos = 100000;
        
//        @Comment("TODO")
//        boolean enableCompactGPUFormats = false;
        
        @Comment("TODO")
        boolean hdLightmaps = false;
        
        @Comment("TODO")
        boolean lightmapNoise = false;
        
        @Comment("TODO")
        DiffuseMode diffuseShadingMode = DiffuseMode.NORMAL;
        
        @Comment("TODO")
        boolean lightSmoothing = false;
        
        @Comment("TODO")
        AoMode aoShadingMode = AoMode.NORMAL;
        
//        @Comment("TODO")
//        boolean enableSinglePassCutout = true;
        
        @Comment("TODO")
        boolean fastChunkOcclusion = true;
        
        @Comment("TODO")
        boolean batchedChunkRender = true;
        
//        @Comment("TODO")
//        boolean disableVanillaChunkMatrix = true;
        
        @Comment("TODO")
        boolean preventDepthFighting = true;

        // DEBUG
        @Comment("Output runtime per-material shader source. For shader development debugging.")
        boolean shaderDebug = false;
        
        @Comment("TODO")        
        boolean lightmapDebug = false;
        
        @Comment("TODO")        
        boolean conciseErrors = true;
        
        @Comment("TODO")        
        boolean logMachineInfo = true;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static boolean itemShaderRender = DEFAULTS.itemShaderRender;
    public static boolean hardcoreDarkness = DEFAULTS.hardcoreDarkness;
    public static boolean subtleFog = DEFAULTS.subtleFog;
    public static boolean shaderDebug = DEFAULTS.shaderDebug;
    public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
    
    public static boolean hdLightmaps = DEFAULTS.hdLightmaps;
    public static boolean lightmapNoise = DEFAULTS.lightmapNoise;
    public static DiffuseMode diffuseShadingMode = DEFAULTS.diffuseShadingMode;
    public static boolean lightSmoothing = DEFAULTS.lightSmoothing;
    public static AoMode aoShadingMode = DEFAULTS.aoShadingMode;
    
    public static long minChunkBudgetNanos = DEFAULTS.minChunkBudgetNanos;
    public static boolean enableCompactGPUFormats = false; //DEFAULTS.enableCompactGPUFormats;
    
    public static boolean enableSinglePassCutout = false; //DEFAULTS.enableSinglePassCutout;
    public static boolean fastChunkOcclusion = DEFAULTS.fastChunkOcclusion;
    public static boolean batchedChunkRender = DEFAULTS.batchedChunkRender;
    public static boolean disableVanillaChunkMatrix = false; //DEFAULTS.disableVanillaChunkMatrix;
    public static boolean preventDepthFighting = DEFAULTS.preventDepthFighting;
    public static boolean lightmapDebug = DEFAULTS.lightmapDebug;
    public static boolean conciseErrors = DEFAULTS.conciseErrors;
    public static boolean logMachineInfo = DEFAULTS.logMachineInfo;
    
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
        itemShaderRender = config.itemShaderRender;
        hardcoreDarkness = config.hardcoreDarkness;
        subtleFog = config.subtleFog;
        shaderDebug = config.shaderDebug;
//        enableCompactGPUFormats = config.enableCompactGPUFormats;
        minChunkBudgetNanos = config.minChunkBudgetNanos;
        maxLightmapDelayFrames = config.maxLightmapDelayFrames;
        
        hdLightmaps = config.hdLightmaps;
        lightmapNoise = config.lightmapNoise;
        diffuseShadingMode = config.diffuseShadingMode;
        lightSmoothing = config.lightSmoothing;
        aoShadingMode = config.aoShadingMode;
        
//        enableSinglePassCutout = config.enableSinglePassCutout;
        fastChunkOcclusion = config.fastChunkOcclusion;
        batchedChunkRender = config.batchedChunkRender;
//        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
        preventDepthFighting = config.preventDepthFighting;
        
        lightmapDebug = config.lightmapDebug;
        conciseErrors = config.conciseErrors;
        logMachineInfo = config.logMachineInfo;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.itemShaderRender = itemShaderRender;
        config.hardcoreDarkness = hardcoreDarkness;
        config.subtleFog = subtleFog;
        config.shaderDebug = shaderDebug;
//        config.enableCompactGPUFormats = enableCompactGPUFormats;
        config.minChunkBudgetNanos = minChunkBudgetNanos;
        config.maxLightmapDelayFrames = maxLightmapDelayFrames;
        
        config.hdLightmaps = hdLightmaps;
        config.lightmapNoise = lightmapNoise;
        config.diffuseShadingMode = diffuseShadingMode;
        config.lightSmoothing = lightSmoothing;
        config.aoShadingMode = aoShadingMode; 
        
//        config.enableSinglePassCutout = enableSinglePassCutout;
        config.fastChunkOcclusion = fastChunkOcclusion;
        config.batchedChunkRender = batchedChunkRender;
//        config.disableVanillaChunkMatrix = disableVanillaChunkMatrix;
        config.preventDepthFighting = preventDepthFighting;
        
        config.lightmapDebug = lightmapDebug;
        config.conciseErrors = conciseErrors;
        config.logMachineInfo = logMachineInfo;
        
        try {
            String result = JANKSON.toJson(config).toJson(true, true, 0);
            if (!configFile.exists())
                configFile.createNewFile();
            
            try(
                    FileOutputStream out = new FileOutputStream(configFile, false);
            ) {
                out.write(result.getBytes());
                out.flush();
                out.close();
            }
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
        
        features.addOption(new BooleanListEntry("config.canvas.value.item_render", itemShaderRender, "config.canvas.reset", 
                () -> DEFAULTS.itemShaderRender, b -> itemShaderRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        features.addOption(new BooleanListEntry("config.canvas.value.hardcore_darkness", hardcoreDarkness, "config.canvas.reset", 
                () -> DEFAULTS.hardcoreDarkness, b -> {hardcoreDarkness = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hardcore_darkness").split(";"))));
        
        features.addOption(new BooleanListEntry("config.canvas.value.subtle_fog", subtleFog, "config.canvas.reset", 
                () -> DEFAULTS.subtleFog, b -> {subtleFog = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.subtle_fog").split(";"))));
        
        // LIGHTING
        ConfigScreenBuilder.CategoryBuilder lighting = builder.addCategory("config.canvas.category.lighting");
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.light_smoothing", lightSmoothing, "config.canvas.reset", 
                () -> DEFAULTS.lightSmoothing, b -> {lightSmoothing = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.light_smoothing").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.hd_lightmaps", hdLightmaps, "config.canvas.reset", 
                () -> DEFAULTS.hdLightmaps, b -> {hdLightmaps = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hd_lightmaps").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.lightmap_noise", lightmapNoise, "config.canvas.reset", 
                () -> DEFAULTS.lightmapNoise, b -> {lightmapNoise = b; reloadShaders = true;}, 
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
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.compact_gpu_formats", enableCompactGPUFormats, "config.canvas.reset", 
//                () -> DEFAULTS.enableCompactGPUFormats, b -> enableCompactGPUFormats = b, 
//                () -> Optional.of(I18n.translate("config.canvas.help.compact_gpu_formats").split(";"))));
        
        tweaks.addOption(new LongListEntry("config.canvas.value.min_chunk_budget", minChunkBudgetNanos, "config.canvas.reset", 
                () -> DEFAULTS.minChunkBudgetNanos, l -> minChunkBudgetNanos = l, 
                () -> Optional.of(I18n.translate("config.canvas.help.min_chunk_budget").split(";"))));
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.single_pass_cutout", enableSinglePassCutout, "config.canvas.reset", 
//                () -> DEFAULTS.enableSinglePassCutout, b -> enableSinglePassCutout = b, 
//                () -> Optional.of(I18n.translate("config.canvas.help.single_pass_cutout").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.chunk_occlusion", fastChunkOcclusion, "config.canvas.reset", 
                () -> DEFAULTS.fastChunkOcclusion, b -> fastChunkOcclusion = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.chunk_occlusion").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.batch_chunk_render", batchedChunkRender, "config.canvas.reset", 
                () -> DEFAULTS.batchedChunkRender, b -> batchedChunkRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.batch_chunk_render").split(";"))));
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.vanilla_chunk_matrix", disableVanillaChunkMatrix, "config.canvas.reset", 
//                () -> DEFAULTS.disableVanillaChunkMatrix, b -> disableVanillaChunkMatrix = b, 
//                () -> Optional.of(I18n.translate("config.canvas.help.vanilla_chunk_matrix").split(";"))));
        
        tweaks.addOption(new BooleanListEntry("config.canvas.value.adjust_vanilla_geometry", preventDepthFighting, "config.canvas.reset", 
                () -> DEFAULTS.preventDepthFighting, b -> {preventDepthFighting = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.adjust_vanilla_geometry").split(";"))));
        
        
        // DEBUG
        ConfigScreenBuilder.CategoryBuilder debug = builder.addCategory("config.canvas.category.debug");
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug", shaderDebug, "config.canvas.reset", 
                () -> DEFAULTS.shaderDebug, b -> shaderDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug_lightmap", lightmapDebug, "config.canvas.reset", 
                () -> DEFAULTS.lightmapDebug, b -> lightmapDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug_lightmap").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.concise_errors", conciseErrors, "config.canvas.reset", 
                () -> DEFAULTS.conciseErrors, b -> conciseErrors = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.concise_errors").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.log_machine_info", logMachineInfo, "config.canvas.reset", 
                () -> DEFAULTS.logMachineInfo, b -> logMachineInfo = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.log_machine_info").split(";"))));
        
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
