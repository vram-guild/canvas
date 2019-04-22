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
    }
    
    static final ConfigData DEFAULTS = new ConfigData();
    private static final Gson GSON = new GsonBuilder().create();
    private static final Jankson JANKSON = Jankson.builder().build();
    
    public static int maxPipelines = DEFAULTS.maxPipelines;
    public static boolean enableItemRender = DEFAULTS.enableItemRender;
    public static boolean enableShaderDebug = DEFAULTS.enableShaderDebug;
    
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
        maxPipelines = config.maxPipelines;
    }

    private static void saveConfig() {
        ConfigData config = new ConfigData();
        config.enableItemRender = enableItemRender;
        config.enableShaderDebug = enableShaderDebug;
        config.maxPipelines = maxPipelines;
        
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
        
        rendering.addOption(new BooleanListEntry("config.canvas.value.item_render", enableItemRender, "config.canvas.reset", 
                () -> DEFAULTS.enableItemRender, b -> enableItemRender = b, 
                () -> Optional.of(I18n.translate("config.canvas.help.item_render").split(";"))));
        
        rendering.addOption(new IntegerSliderEntry("config.canvas.value.max_materials", 128, 4096, maxPipelines, "config.canvas.reset", 
                () -> DEFAULTS.maxPipelines, i -> maxPipelines = i, 
                () -> Optional.of(I18n.translate("config.canvas.help.max_materials").split(";"))));
        
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
        maxPipelines = MathHelper.smallestEncompassingPowerOfTwo(maxPipelines);
        saveConfig();
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
