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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.fluid.FluidHandler;
import grondag.canvas.compat.Compat;
import grondag.canvas.config.ConfigManager;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.RenderLayerExt;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.texture.ResourceCacheManager;
import grondag.frex.api.RendererFeature;
import grondag.frex.api.fluid.FluidQuadSupplier;

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
	public static KeyBinding DEBUG_TOGGLE = new KeyBinding("key.canvas.debug_toggle", Character.valueOf('`'), "key.canvas.category");
	public static KeyBinding DEBUG_PREV = new KeyBinding("key.canvas.debug_prev", Character.valueOf('['), "key.canvas.category");
	public static KeyBinding DEBUG_NEXT = new KeyBinding("key.canvas.debug_next", Character.valueOf(']'), "key.canvas.category");
	public static KeyBinding RECOMPILE = new KeyBinding("key.canvas.recompile", Character.valueOf('='), "key.canvas.category");
	public static KeyBinding FLAWLESS_TOGGLE = new KeyBinding("key.canvas.flawless_toggle", -1, "key.canvas.category");
	public static KeyBinding PROFILER_TOGGLE = new KeyBinding("key.canvas.profiler_toggle", -1, "key.canvas.category");
	public static String versionString = "unknown";

	@Override
	public void onInitializeClient() {
		versionString = FabricLoader.getInstance().getModContainer(CanvasMod.MODID).get().getMetadata().getVersion().getFriendlyString();

		ConfigManager.init();
		RendererAccess.INSTANCE.registerRenderer(Canvas.INSTANCE);
		RendererFeature.registerFeatures(RendererFeature.UPDATE_MATERIAL_REGISTRATION);
		FluidQuadSupplier.setReloadHandler(FluidHandler.HANDLER);
		InvalidateRenderStateCallback.EVENT.register(Canvas.INSTANCE::reload);

		if (Configurator.debugNativeMemoryAllocation) {
			LOG.warn("Canvas is configured to enable native memory debug. This WILL cause slow performance and other issues.  Debug output will print at game exit.");
			Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		}

		((RenderLayerExt) RenderLayer.getTranslucent()).canvas_blendMode(BlendMode.TRANSLUCENT);
		((RenderLayerExt) RenderLayer.getTripwire()).canvas_blendMode(BlendMode.TRANSLUCENT);
		((RenderLayerExt) RenderLayer.getSolid()).canvas_blendMode(BlendMode.SOLID);
		((RenderLayerExt) RenderLayer.getCutout()).canvas_blendMode(BlendMode.CUTOUT);
		((RenderLayerExt) RenderLayer.getCutoutMipped()).canvas_blendMode(BlendMode.CUTOUT_MIPPED);

		KeyBindingHelper.registerKeyBinding(DEBUG_TOGGLE);
		KeyBindingHelper.registerKeyBinding(DEBUG_PREV);
		KeyBindingHelper.registerKeyBinding(DEBUG_NEXT);
		KeyBindingHelper.registerKeyBinding(RECOMPILE);
		KeyBindingHelper.registerKeyBinding(FLAWLESS_TOGGLE);
		KeyBindingHelper.registerKeyBinding(PROFILER_TOGGLE);

		Compat.init();

		FabricLoader.getInstance().getModContainer(MODID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("canvas:default"), "resourcepacks/canvas_default", modContainer, true);
			ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("canvas:extras"), "resourcepacks/canvas_extras", modContainer, false);
			//ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("canvas:development"), "resourcepacks/canvas_wip", modContainer, false);
		});

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(PipelineLoader.INSTANCE);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ResourceCacheManager.cacheReloader);
	}
}
