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
import me.shedaniel.cloth.gui.entries.IntegerSliderEntry;
import me.shedaniel.cloth.gui.entries.LongListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

// UGLY: this is a rushed mess
@Environment(EnvType.CLIENT)
public class Configurator implements ModMenuApi {
    
    @SuppressWarnings("hiding")
    static class ConfigData {
        @Comment("Increase if 'Max shader material exceeded' error occurs. Larger values consume a small amount of memory.")
        int maxPipelines = 128;
        
        @Comment("Output runtime per-material shader source. For shader development debugging.")
        boolean enableShaderDebug = false;
        
        @Comment("Applies material properties and shaders to items. (WIP)")
        boolean enableItemRender = false;
        
        //TODO: docs
        @Comment("TODO")
        int maxLightmapDelayFrames = 0;
        
        @Comment("TODO")
        long minChunkBudgetNanos = 200000;
        
        @Comment("TODO")
        boolean enableCompactGPUFormats = true;
        
        @Comment("TODO")
        boolean enableHdLightmaps = false;
        
        @Comment("TODO")
        boolean enableLightmapNoise = false;
        
        @Comment("TODO")
        boolean enableDiffuseShading = true;
        
        @Comment("TODO")
        boolean enableLightSmoothing = false;
        
        @Comment("TODO")
        boolean enableSubtleAo = true;
        
        @Comment("TODO")
        boolean enableAoShading = true;
        
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
        
        @Comment("TODO")        
        boolean enableLightmapDebug = false;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static int maxShaders = DEFAULTS.maxPipelines;
    public static boolean enableItemRender = DEFAULTS.enableItemRender;
    public static boolean enableShaderDebug = DEFAULTS.enableShaderDebug;
    public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
    
    public static boolean enableHdLightmaps = DEFAULTS.enableHdLightmaps;
    public static boolean enableLightmapNoise = DEFAULTS.enableLightmapNoise;
    public static boolean enableDiffuseShading = DEFAULTS.enableDiffuseShading;
    public static boolean enableLightSmoothing = DEFAULTS.enableLightSmoothing;
    public static boolean enableSubtleAo = DEFAULTS.enableSubtleAo;
    public static boolean enableAoShading = DEFAULTS.enableAoShading;
    
    public static long minChunkBudgetNanos = DEFAULTS.minChunkBudgetNanos;
    public static boolean enableCompactGPUFormats = DEFAULTS.enableCompactGPUFormats;
    
    public static boolean enableSinglePassCutout = DEFAULTS.enableSinglePassCutout;
    public static boolean enableImprovedChunkOcclusion = DEFAULTS.enableImprovedChunkOcclusion;
    public static boolean enableBatchedChunkRender = DEFAULTS.enableBatchedChunkRender;
    public static boolean disableVanillaChunkMatrix = DEFAULTS.disableVanillaChunkMatrix;
    public static boolean adjustVanillaModelGeometry = DEFAULTS.adjustVanillaModelGeometry;
    public static boolean enableLightmapDebug = DEFAULTS.enableLightmapDebug;
    

    
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
            Canvas.LOG.error("Unable to load config. Using default values.");
        }
        enableItemRender = config.enableItemRender;
        enableShaderDebug = config.enableShaderDebug;
        maxShaders = config.maxPipelines;
        enableCompactGPUFormats = config.enableCompactGPUFormats;
        minChunkBudgetNanos = config.minChunkBudgetNanos;
        maxLightmapDelayFrames = config.maxLightmapDelayFrames;
        
        enableHdLightmaps = config.enableHdLightmaps;
        enableLightmapNoise = config.enableLightmapNoise;
        enableDiffuseShading = config.enableDiffuseShading;
        enableLightSmoothing = config.enableLightSmoothing;
        enableAoShading = config.enableAoShading;
        enableSubtleAo = config.enableSubtleAo;
        
        enableSinglePassCutout = config.enableSinglePassCutout;
        enableImprovedChunkOcclusion = config.enableImprovedChunkOcclusion;
        enableBatchedChunkRender = config.enableBatchedChunkRender;
        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
        adjustVanillaModelGeometry = config.adjustVanillaModelGeometry;
        
        enableLightmapDebug = config.enableLightmapDebug;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.enableItemRender = enableItemRender;
        config.enableShaderDebug = enableShaderDebug;
        config.maxPipelines = maxShaders;
        config.enableCompactGPUFormats = enableCompactGPUFormats;
        config.minChunkBudgetNanos = minChunkBudgetNanos;
        config.maxLightmapDelayFrames = maxLightmapDelayFrames;
        
        config.enableHdLightmaps = enableHdLightmaps;
        config.enableLightmapNoise = enableLightmapNoise;
        config.enableDiffuseShading = enableDiffuseShading;
        config.enableLightSmoothing = enableLightSmoothing;
        config.enableAoShading = enableAoShading; 
        config.enableSubtleAo = enableSubtleAo;
        
        config.enableSinglePassCutout = enableSinglePassCutout;
        config.enableImprovedChunkOcclusion = enableImprovedChunkOcclusion;
        config.enableBatchedChunkRender = enableBatchedChunkRender;
        config.disableVanillaChunkMatrix = disableVanillaChunkMatrix;
        config.adjustVanillaModelGeometry = adjustVanillaModelGeometry;
        
        config.enableLightmapDebug = enableLightmapDebug;
        
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
            Canvas.LOG.error("Unable to save config.");
            return;
        }
    }
    
    static boolean reloadTerrain = false;
    static boolean reloadShaders = false;
    
    private static Screen display() {
        reloadTerrain = false;
        reloadShaders = false;
        
        ConfigScreenBuilder builder = ConfigScreenBuilder.create(screenIn, "config.canvas.title", null);
        
        // RENDERING
        ConfigScreenBuilder.CategoryBuilder rendering = builder.addCategory("config.canvas.category.rendering");
        
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.compact_gpu_formats", enableCompactGPUFormats, "config.canvas.reset", 
                () -> DEFAULTS.enableCompactGPUFormats, b -> enableCompactGPUFormats = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.compact_gpu_formats").split(";"))));
        
        rendering.addOption(new LongListEntry("config.canvas.value.min_chunk_budget", minChunkBudgetNanos, "config.canvas.reset", 
                () -> DEFAULTS.minChunkBudgetNanos, l -> minChunkBudgetNanos = l, 
                () -> Optional.of(I18n.translate("config.canvas.help.min_chunk_budget").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.item_render", enableItemRender, "config.canvas.reset", 
                () -> DEFAULTS.enableItemRender, b -> enableItemRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        rendering.addOption(new IntegerSliderEntry("config.canvas.value.max_materials", 128, 4096, maxShaders, "config.canvas.reset", 
                () -> DEFAULTS.maxPipelines, i -> maxShaders = i, 
                () -> Optional.of(I18n.translate("config.canvas.help.max_materials").split(";"))));
        
        ///

        
        rendering.addOption(new IntegerSliderEntry("config.canvas.value.lightmap_delay_frames", 0, 20, maxLightmapDelayFrames, "config.canvas.reset", 
                () -> DEFAULTS.maxLightmapDelayFrames, b -> maxLightmapDelayFrames = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_delay_frames").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.single_pass_cutout", enableSinglePassCutout, "config.canvas.reset", 
                () -> DEFAULTS.enableSinglePassCutout, b -> enableSinglePassCutout = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.single_pass_cutout").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.chunk_occlusion", enableImprovedChunkOcclusion, "config.canvas.reset", 
                () -> DEFAULTS.enableImprovedChunkOcclusion, b -> enableImprovedChunkOcclusion = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.chunk_occlusion").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.batch_chunk_render", enableBatchedChunkRender, "config.canvas.reset", 
                () -> DEFAULTS.enableBatchedChunkRender, b -> enableBatchedChunkRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.batch_chunk_render").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.vanilla_chunk_matrix", disableVanillaChunkMatrix, "config.canvas.reset", 
                () -> DEFAULTS.disableVanillaChunkMatrix, b -> disableVanillaChunkMatrix = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.vanilla_chunk_matrix").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.adjust_vanilla_geometry", adjustVanillaModelGeometry, "config.canvas.reset", 
                () -> DEFAULTS.adjustVanillaModelGeometry, b -> adjustVanillaModelGeometry = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.adjust_vanilla_geometry").split(";"))));
        
        ConfigScreenBuilder.CategoryBuilder lighting = builder.addCategory("config.canvas.category.lighting");
        
        // LIGHTING
        lighting.addOption(new BooleanListEntry("config.canvas.value.light_smoothing", enableLightSmoothing, "config.canvas.reset", 
                () -> DEFAULTS.enableLightSmoothing, b -> {enableLightSmoothing = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.light_smoothing").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.smooth_lightmaps", enableHdLightmaps, "config.canvas.reset", 
                () -> DEFAULTS.enableHdLightmaps, b -> {enableHdLightmaps = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.smooth_lightmaps").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.subtle_ao", enableSubtleAo, "config.canvas.reset", 
                () -> DEFAULTS.enableSubtleAo, b -> {enableSubtleAo = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.subtle_ao").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.lightmap_noise", enableLightmapNoise, "config.canvas.reset", 
                () -> DEFAULTS.enableLightmapNoise, b -> {enableLightmapNoise = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_noise").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.diffuse_shading", enableDiffuseShading, "config.canvas.reset", 
                () -> DEFAULTS.enableDiffuseShading, b -> {enableDiffuseShading = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.diffuse_shading").split(";"))));
        
        lighting.addOption(new BooleanListEntry("config.canvas.value.ao_shading", enableAoShading, "config.canvas.reset", 
                () -> DEFAULTS.enableAoShading, b -> {enableAoShading = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.ao_shading").split(";"))));
        
        
        // DEBUG
        ConfigScreenBuilder.CategoryBuilder debug = builder.addCategory("config.canvas.category.debug");
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug", enableShaderDebug, "config.canvas.reset", 
                () -> DEFAULTS.enableShaderDebug, b -> enableShaderDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug").split(";"))));
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug_lightmap", enableLightmapDebug, "config.canvas.reset", 
                () -> DEFAULTS.enableLightmapDebug, b -> enableLightmapDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug_lightmap").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        builder.setOnSave(Configurator::saveUserInput);
        
        return builder.build();
    }
    
    private static void saveUserInput(SavedConfig config) {
        maxShaders = MathHelper.smallestEncompassingPowerOfTwo(maxShaders);
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
