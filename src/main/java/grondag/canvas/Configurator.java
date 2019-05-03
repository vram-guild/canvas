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
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.cloth.api.ConfigScreenBuilder;
import me.shedaniel.cloth.api.ConfigScreenBuilder.SavedConfig;
import me.shedaniel.cloth.gui.entries.BooleanListEntry;
import me.shedaniel.cloth.gui.entries.IntegerSliderEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
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
        boolean preventTerrainShadingAnisotropy = false;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableCompactGPUFormats = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableSmoothLightmaps = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableLightmapNoise= false;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableSinglePassCutout = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableImprovedChunkOcclusion = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean enableBatchedChunkRender = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean disableVanillaChunkMatrix = true;
        
        //TODO: docs
        @Comment("TODO")
        boolean adjustVanillaModelGeometry = true;
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static int maxShaders = DEFAULTS.maxPipelines;
    public static boolean enableItemRender = DEFAULTS.enableItemRender;
    public static boolean enableShaderDebug = DEFAULTS.enableShaderDebug;
    public static boolean preventTerrainShadingAnisotropy = DEFAULTS.preventTerrainShadingAnisotropy;
    public static boolean enableCompactGPUFormats = DEFAULTS.enableCompactGPUFormats;
    
    public static boolean enableSmoothLightmaps = DEFAULTS.enableSmoothLightmaps;
    public static boolean enableLightmapNoise = DEFAULTS.enableLightmapNoise;
    public static boolean enableSinglePassCutout = DEFAULTS.enableSinglePassCutout;
    public static boolean enableImprovedChunkOcclusion = DEFAULTS.enableImprovedChunkOcclusion;
    public static boolean enableBatchedChunkRender = DEFAULTS.enableBatchedChunkRender;
    public static boolean disableVanillaChunkMatrix = DEFAULTS.disableVanillaChunkMatrix;
    public static boolean adjustVanillaModelGeometry = DEFAULTS.adjustVanillaModelGeometry;
    
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
        preventTerrainShadingAnisotropy = config.preventTerrainShadingAnisotropy;
        enableCompactGPUFormats = config.enableCompactGPUFormats;
        
        enableSmoothLightmaps = config.enableSmoothLightmaps;
        enableLightmapNoise = config.enableLightmapNoise;
        enableSinglePassCutout = config.enableSinglePassCutout;
        enableImprovedChunkOcclusion = config.enableImprovedChunkOcclusion;
        enableBatchedChunkRender = config.enableBatchedChunkRender;
        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
        adjustVanillaModelGeometry = config.adjustVanillaModelGeometry;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.enableItemRender = enableItemRender;
        config.enableShaderDebug = enableShaderDebug;
        config.maxPipelines = maxShaders;
        config.preventTerrainShadingAnisotropy = preventTerrainShadingAnisotropy;
        config.enableCompactGPUFormats = enableCompactGPUFormats;
        
        config.enableSmoothLightmaps = enableSmoothLightmaps;
        config.enableLightmapNoise = enableLightmapNoise;
        config.enableSinglePassCutout = enableSinglePassCutout;
        config.enableImprovedChunkOcclusion = enableImprovedChunkOcclusion;
        config.enableBatchedChunkRender = enableBatchedChunkRender;
        config.disableVanillaChunkMatrix = disableVanillaChunkMatrix;
        config.adjustVanillaModelGeometry = adjustVanillaModelGeometry;
        
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
    
    private static Screen display() {
        ConfigScreenBuilder builder = ConfigScreenBuilder.create(screenIn, "config.canvas.title", null);
        
        // RENDERING
        ConfigScreenBuilder.CategoryBuilder rendering = builder.addCategory("config.canvas.category.rendering");
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.prevent_anisotropy", preventTerrainShadingAnisotropy, "config.canvas.reset", 
                () -> DEFAULTS.preventTerrainShadingAnisotropy, b -> preventTerrainShadingAnisotropy = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.prevent_anisotropy").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.compact_gpu_formats", enableCompactGPUFormats, "config.canvas.reset", 
                () -> DEFAULTS.enableCompactGPUFormats, b -> enableCompactGPUFormats = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.compact_gpu_formats").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.item_render", enableItemRender, "config.canvas.reset", 
                () -> DEFAULTS.enableItemRender, b -> enableItemRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        rendering.addOption(new IntegerSliderEntry("config.canvas.value.max_materials", 128, 4096, maxShaders, "config.canvas.reset", 
                () -> DEFAULTS.maxPipelines, i -> maxShaders = i, 
                () -> Optional.of(I18n.translate("config.canvas.help.max_materials").split(";"))));
        
        ///
        rendering.addOption(new BooleanListEntry("config.canvas.value.smooth_lightmaps", enableSmoothLightmaps, "config.canvas.reset", 
                () -> DEFAULTS.enableSmoothLightmaps, b -> enableSmoothLightmaps = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.smooth_lightmaps").split(";"))));
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.lightmap_noise", enableLightmapNoise, "config.canvas.reset", 
                () -> DEFAULTS.enableLightmapNoise, b -> enableLightmapNoise = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.lightmap_noise").split(";"))));
        
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
        
        // DEBUG
        ConfigScreenBuilder.CategoryBuilder debug = builder.addCategory("config.canvas.category.debug");
        
        debug.addOption(new BooleanListEntry("config.canvas.value.shader_debug", enableShaderDebug, "config.canvas.reset", 
                () -> DEFAULTS.enableShaderDebug, b -> enableShaderDebug = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.shader_debug").split(";"))));
        
        builder.setDoesConfirmSave(false);
        
        builder.setOnSave(Configurator::saveUserInput);
        
        return builder.build();
    }
    
    private static void saveUserInput(SavedConfig config) {
        maxShaders = MathHelper.smallestEncompassingPowerOfTwo(maxShaders);
        saveConfig();
        
        //TODO: detect and force chunk rebuild if needed
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
