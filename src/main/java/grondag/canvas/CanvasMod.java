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

package grondag.canvas;

import io.vram.frex.api.config.FrexFeature;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.model.FluidModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.fluid.FluidHandler;
import grondag.canvas.compat.Compat;
import grondag.canvas.config.ConfigManager;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.texture.ResourceCacheManager;

//FEAT: weather rendering
//FEAT: sky rendering
//FEAT: pbr textures
//PERF: disable animated textures when not in view
//PERF: improve light smoothing performance
//FEAT: colored lights
//FEAT: weather uniforms
//FEAT: biome texture in shader

public class CanvasMod implements ClientModInitializer {
	public static final String MODID = "canvas";
	public static final Logger LOG = LogManager.getLogger("Canvas");
	public static KeyMapping DEBUG_TOGGLE = new KeyMapping("key.canvas.debug_toggle", Character.valueOf('`'), "key.canvas.category");
	public static KeyMapping DEBUG_PREV = new KeyMapping("key.canvas.debug_prev", Character.valueOf('['), "key.canvas.category");
	public static KeyMapping DEBUG_NEXT = new KeyMapping("key.canvas.debug_next", Character.valueOf(']'), "key.canvas.category");
	public static KeyMapping RECOMPILE = new KeyMapping("key.canvas.recompile", Character.valueOf('='), "key.canvas.category");
	public static KeyMapping FLAWLESS_TOGGLE = new KeyMapping("key.canvas.flawless_toggle", -1, "key.canvas.category");
	public static KeyMapping PROFILER_TOGGLE = new KeyMapping("key.canvas.profiler_toggle", -1, "key.canvas.category");
	public static String versionString = "unknown";

	@Override
	public void onInitializeClient() {
		versionString = FabricLoader.getInstance().getModContainer(CanvasMod.MODID).get().getMetadata().getVersion().getFriendlyString();

		ConfigManager.init();
		FrexFeature.registerFeatures(FrexFeature.UPDATE_MATERIAL_REGISTRATION);

		if (Configurator.debugNativeMemoryAllocation) {
			LOG.warn("Canvas is configured to enable native memory debug. This WILL cause slow performance and other issues.  Debug output will print at game exit.");
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		}

		((RenderLayerExt) RenderType.translucent()).canvas_preset(MaterialConstants.PRESET_TRANSLUCENT);
		((RenderLayerExt) RenderType.tripwire()).canvas_preset(MaterialConstants.PRESET_TRANSLUCENT);
		((RenderLayerExt) RenderType.solid()).canvas_preset(MaterialConstants.PRESET_SOLID);
		((RenderLayerExt) RenderType.cutout()).canvas_preset(MaterialConstants.PRESET_CUTOUT);
		((RenderLayerExt) RenderType.cutoutMipped()).canvas_preset(MaterialConstants.PRESET_CUTOUT_MIPPED);

		platformSpecificInit();

		Compat.init();
	}

	// Things that need to be moved to proxy or architectury, etc.
	private static void platformSpecificInit() {
		FluidModel.setReloadHandler(FluidHandler.HANDLER);

		InvalidateRenderStateCallback.EVENT.register(Canvas.instance()::reload);

		KeyBindingHelper.registerKeyBinding(DEBUG_TOGGLE);
		KeyBindingHelper.registerKeyBinding(DEBUG_PREV);
		KeyBindingHelper.registerKeyBinding(DEBUG_NEXT);
		KeyBindingHelper.registerKeyBinding(RECOMPILE);
		KeyBindingHelper.registerKeyBinding(FLAWLESS_TOGGLE);
		KeyBindingHelper.registerKeyBinding(PROFILER_TOGGLE);

		FabricLoader.getInstance().getModContainer(MODID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:canvas_default"), modContainer, ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:canvas_extras"), modContainer, ResourcePackActivationType.NORMAL);
			//ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("canvas:development"), "resourcepacks/canvas_wip", modContainer, false);
		});

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(PipelineLoader.INSTANCE);
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ResourceCacheManager.cacheReloader);
	}
}
