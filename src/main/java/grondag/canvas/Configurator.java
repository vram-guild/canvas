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

import grondag.canvas.material.ShaderManager;
import grondag.fermion.shadow.jankson.Comment;
import grondag.fermion.shadow.jankson.Jankson;
import grondag.fermion.shadow.jankson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

@Environment(EnvType.CLIENT)
public class Configurator {
    
    @SuppressWarnings("hiding")
    static class ConfigData {
        @Comment("Applies material properties and shaders to items. (WIP)")
        boolean itemShaderRender = false;
        
        @Comment("Reduces terrain lighting to full darkness in absence of moon/torch light.")
        boolean hardcoreDarkness = false;
        
        @Comment("Makes terrain fog a little less foggy.")
        boolean subtleFog = false;
        
//        @Comment("TODO")
//        boolean enableCompactGPUFormats = false;
        
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
        
        @Comment("Render cutout and solid layers in a single pass.")
        boolean enableSinglePassCutout = true;
        
        @Comment("Helps with chunk rebuild and also rendering when player is moving or many blocks update.")
        boolean fastChunkOcclusion = true;
        
        @Comment("Draws multiple chunks with same view transformation. Much faster, but try without if you see visual defects.")
        boolean batchedChunkRender = true;
        
//        @Comment("TODO")
//        boolean disableVanillaChunkMatrix = true;
        
        @Comment("Adjusts quads on some vanilla models (like iron bars) to avoid z-fighting with neighbor blocks.")
        boolean preventDepthFighting = true;
        
        @Comment("Forces game to allow up to this many nanoseconds for chunk loading each frame. May prevent chunk load delay at high FPS.")
        long minChunkBudgetNanos = 100000;
        
        @Comment("Treats model geometry outside of block boundaries as on the block for lighting purposes. Helps prevent bad lighting outcomes.")
        boolean clampExteriorVertices = true;
        
//        @Comment("Pad vertex data in chunks with multiple formats. Significantly increases frame rate at cost of some wasted memory.")
//        boolean padTranslucentFormats = true;

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
    public static boolean moreLightmap = DEFAULTS.moreLightmap;
    
    public static long minChunkBudgetNanos = DEFAULTS.minChunkBudgetNanos;
    public static boolean enableCompactGPUFormats = false; //DEFAULTS.enableCompactGPUFormats;
    
    public static boolean enableSinglePassCutout = DEFAULTS.enableSinglePassCutout;
    public static boolean fastChunkOcclusion = DEFAULTS.fastChunkOcclusion;
    public static boolean batchedChunkRender = DEFAULTS.batchedChunkRender;
    public static boolean disableVanillaChunkMatrix = false; //DEFAULTS.disableVanillaChunkMatrix;
    public static boolean preventDepthFighting = DEFAULTS.preventDepthFighting;
    public static boolean clampExteriorVertices = DEFAULTS.clampExteriorVertices;
//    public static boolean padTranslucentFormats = DEFAULTS.padTranslucentFormats;
    
    public static boolean lightmapDebug = DEFAULTS.lightmapDebug;
    public static boolean conciseErrors = DEFAULTS.conciseErrors;
    public static boolean logMachineInfo = DEFAULTS.logMachineInfo;
    public static boolean logGlStateChanges = DEFAULTS.logGlStateChanges;
    
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
        moreLightmap = config.moreLightmap;
        
        hdLightmaps = config.hdLightmaps;
        lightmapNoise = config.lightmapNoise;
        diffuseShadingMode = config.diffuseShadingMode;
        lightSmoothing = config.lightSmoothing;
        aoShadingMode = config.aoShadingMode;
        
        
        enableSinglePassCutout = config.enableSinglePassCutout;
        fastChunkOcclusion = config.fastChunkOcclusion;
        batchedChunkRender = config.batchedChunkRender;
//        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
        preventDepthFighting = config.preventDepthFighting;
        clampExteriorVertices = config.clampExteriorVertices;
//        padTranslucentFormats = config.padTranslucentFormats;
        
        lightmapDebug = config.lightmapDebug;
        conciseErrors = config.conciseErrors;
        logMachineInfo = config.logMachineInfo;
        logGlStateChanges = config.logGlStateChanges;
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
        config.moreLightmap = moreLightmap;
        
        config.enableSinglePassCutout = enableSinglePassCutout;
        config.fastChunkOcclusion = fastChunkOcclusion;
        config.batchedChunkRender = batchedChunkRender;
//        config.disableVanillaChunkMatrix = disableVanillaChunkMatrix;
        config.preventDepthFighting = preventDepthFighting;
        config.clampExteriorVertices = clampExteriorVertices;
//        config.padTranslucentFormats = padTranslucentFormats;
        
        config.lightmapDebug = lightmapDebug;
        config.conciseErrors = conciseErrors;
        config.logMachineInfo = logMachineInfo;
        config.logGlStateChanges = logGlStateChanges;
        
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
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(screenIn).setTitle("config.canvas.title").setSavingRunnable(Configurator::saveUserInput);
        
        // FEATURES
        ConfigCategory features = builder.getOrCreateCategory("config.canvas.category.features");
        
        features.addEntry(new BooleanListEntry("config.canvas.value.item_render", itemShaderRender, "config.canvas.reset", 
                () -> DEFAULTS.itemShaderRender, b -> itemShaderRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.canvas.value.hardcore_darkness", hardcoreDarkness, "config.canvas.reset", 
                () -> DEFAULTS.hardcoreDarkness, b -> {hardcoreDarkness = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hardcore_darkness").split(";"))));
        
        features.addEntry(new BooleanListEntry("config.canvas.value.subtle_fog", subtleFog, "config.canvas.reset", 
                () -> DEFAULTS.subtleFog, b -> {subtleFog = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.subtle_fog").split(";"))));
        
        // LIGHTING
        ConfigCategory lighting = builder.getOrCreateCategory("config.canvas.category.lighting");
        
        lighting.addEntry(new BooleanListEntry("config.canvas.value.light_smoothing", lightSmoothing, "config.canvas.reset", 
                () -> DEFAULTS.lightSmoothing, b -> {lightSmoothing = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.light_smoothing").split(";"))));
        
        lighting.addEntry(new BooleanListEntry("config.canvas.value.hd_lightmaps", hdLightmaps, "config.canvas.reset", 
                () -> DEFAULTS.hdLightmaps, b -> {hdLightmaps = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.hd_lightmaps").split(";"))));
        
        lighting.addEntry(new BooleanListEntry("config.canvas.value.more_lightmap", moreLightmap, "config.canvas.reset", 
                () -> DEFAULTS.moreLightmap, b -> moreLightmap = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.more_lightmap").split(";"))));
        
        lighting.addEntry(new BooleanListEntry("config.canvas.value.lightmap_noise", lightmapNoise, "config.canvas.reset", 
                () -> DEFAULTS.lightmapNoise, b -> {lightmapNoise = b; reloadShaders = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_noise").split(";"))));
        
        lighting.addEntry(new EnumListEntry(
                "config.canvas.value.diffuse_shading", 
                DiffuseMode.class, 
                diffuseShadingMode, 
                "config.canvas.reset", 
                () -> DEFAULTS.diffuseShadingMode, 
                (b) -> {diffuseShadingMode = (DiffuseMode) b; reloadShaders = true;},
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.canvas.help.diffuse_shading").split(";"))));
        
        lighting.addEntry(new EnumListEntry(
                "config.canvas.value.ao_shading", 
                AoMode.class, 
                aoShadingMode, 
                "config.canvas.reset", 
                () -> DEFAULTS.aoShadingMode, 
                (b) -> {aoShadingMode = (AoMode) b; reloadShaders = true;},
                a -> a.toString(),
                () -> Optional.of(I18n.translate("config.canvas.help.ao_shading").split(";"))));
        
        lighting.addEntry(new IntegerSliderEntry("config.canvas.value.lightmap_delay_frames", 0, 20, maxLightmapDelayFrames, "config.canvas.reset", 
                () -> DEFAULTS.maxLightmapDelayFrames, b -> maxLightmapDelayFrames = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_delay_frames").split(";"))));
        
        // TWEAKS
        ConfigCategory tweaks = builder.getOrCreateCategory("config.canvas.category.tweaks");
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.compact_gpu_formats", enableCompactGPUFormats, "config.canvas.reset", 
//                () -> DEFAULTS.enableCompactGPUFormats, b -> enableCompactGPUFormats = b, 
//                () -> Optional.of(I18n.translate("config.canvas.help.compact_gpu_formats").split(";"))));
        
        tweaks.addEntry(new LongListEntry("config.canvas.value.min_chunk_budget", minChunkBudgetNanos, "config.canvas.reset", 
                () -> DEFAULTS.minChunkBudgetNanos, l -> minChunkBudgetNanos = l, 
                () -> Optional.of(I18n.translate("config.canvas.help.min_chunk_budget").split(";"))));
        
        tweaks.addEntry(new BooleanListEntry("config.canvas.value.single_pass_cutout", enableSinglePassCutout, "config.canvas.reset", 
                () -> DEFAULTS.enableSinglePassCutout, b -> {enableSinglePassCutout = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.single_pass_cutout").split(";"))));
        
        tweaks.addEntry(new BooleanListEntry("config.canvas.value.chunk_occlusion", fastChunkOcclusion, "config.canvas.reset", 
                () -> DEFAULTS.fastChunkOcclusion, b -> fastChunkOcclusion = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.chunk_occlusion").split(";"))));
        
        tweaks.addEntry(new BooleanListEntry("config.canvas.value.batch_chunk_render", batchedChunkRender, "config.canvas.reset", 
                () -> DEFAULTS.batchedChunkRender, b -> batchedChunkRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.batch_chunk_render").split(";"))));
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.vanilla_chunk_matrix", disableVanillaChunkMatrix, "config.canvas.reset", 
//                () -> DEFAULTS.disableVanillaChunkMatrix, b -> disableVanillaChunkMatrix = b, 
//                () -> Optional.of(I18n.translate("config.canvas.help.vanilla_chunk_matrix").split(";"))));
        
        tweaks.addEntry(new BooleanListEntry("config.canvas.value.adjust_vanilla_geometry", preventDepthFighting, "config.canvas.reset", 
                () -> DEFAULTS.preventDepthFighting, b -> {preventDepthFighting = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.adjust_vanilla_geometry").split(";"))));
        
        tweaks.addEntry(new BooleanListEntry("config.canvas.value.clamp_exterior_vertices", clampExteriorVertices, "config.canvas.reset", 
                () -> DEFAULTS.clampExteriorVertices, b -> {clampExteriorVertices = b; reloadTerrain = true;}, 
                () -> Optional.of(I18n.translate("config.canvas.help.clamp_exterior_vertices").split(";"))));
        
//        tweaks.addOption(new BooleanListEntry("config.canvas.value.pad_translucent_formats", padTranslucentFormats, "config.canvas.reset", 
//                () -> DEFAULTS.padTranslucentFormats, b -> {padTranslucentFormats = b; reloadTerrain = true;}, 
//                () -> Optional.of(I18n.translate("config.canvas.help.pad_translucent_formats").split(";"))));
        
        // DEBUG
        ConfigCategory debug = builder.getOrCreateCategory("config.canvas.category.debug");
        
        debug.addEntry(new BooleanListEntry("config.canvas.value.shader_debug", shaderDebug, "config.canvas.reset", 
                () -> DEFAULTS.shaderDebug, b -> shaderDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug").split(";"))));
        
        debug.addEntry(new BooleanListEntry("config.canvas.value.shader_debug_lightmap", lightmapDebug, "config.canvas.reset", 
                () -> DEFAULTS.lightmapDebug, b -> lightmapDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug_lightmap").split(";"))));
        
        debug.addEntry(new BooleanListEntry("config.canvas.value.concise_errors", conciseErrors, "config.canvas.reset", 
                () -> DEFAULTS.conciseErrors, b -> conciseErrors = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.concise_errors").split(";"))));
        
        debug.addEntry(new BooleanListEntry("config.canvas.value.log_machine_info", logMachineInfo, "config.canvas.reset", 
                () -> DEFAULTS.logMachineInfo, b -> logMachineInfo = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.log_machine_info").split(";"))));
        
        debug.addEntry(new BooleanListEntry("config.canvas.value.log_gl_state_changes", logGlStateChanges, "config.canvas.reset", 
                () -> DEFAULTS.logGlStateChanges, b -> logGlStateChanges = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.log_gl_state_changes").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        return builder.build();
    }
    
    public static Optional<Supplier<Screen>> getConfigScreen(Screen screen) {
        screenIn = screen;
        return Optional.of(Configurator::display);
    }
    
    public static Screen getRawConfigScreen(Screen screen) {
        screenIn = screen;
        return display();
    }
    
    private static void saveUserInput() {
        saveConfig();
        
        if(reloadTerrain) {
            MinecraftClient.getInstance().worldRenderer.reload();
        } else if(reloadShaders) {
            ShaderManager.INSTANCE.forceReload();
        }
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
