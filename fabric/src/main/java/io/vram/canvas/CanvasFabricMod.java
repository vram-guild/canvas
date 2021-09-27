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

package io.vram.canvas;

import io.vram.frex.api.model.fluid.FluidModel;
import io.vram.frex.api.renderloop.RenderReloadListener;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.fluid.FluidHandler;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.texture.MaterialIndexProvider;
import grondag.canvas.texture.ResourceCacheManager;

public class CanvasFabricMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CanvasMod.versionString = FabricLoader.getInstance().getModContainer(CanvasMod.MODID).get().getMetadata().getVersion().getFriendlyString();

		CanvasMod.init();

		FluidModel.setReloadHandler(FluidHandler.HANDLER);

		RenderReloadListener.register(Canvas.instance()::reload);

		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_TOGGLE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_PREV);
		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_NEXT);
		KeyBindingHelper.registerKeyBinding(CanvasMod.RECOMPILE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.FLAWLESS_TOGGLE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.PROFILER_TOGGLE);

		FabricLoader.getInstance().getModContainer(CanvasMod.MODID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:canvas_default"), modContainer, ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:canvas_extras"), modContainer, ResourcePackActivationType.NORMAL);
			//ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("canvas:development"), "resourcepacks/canvas_wip", modContainer, false);
		});

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return ID;
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				PipelineLoader.reload(manager);
				MaterialIndexProvider.reload();
				ResourceCacheManager.invalidate();
			}
		});
	}

	private static final ResourceLocation ID = new ResourceLocation("canvas:resource_reloader");
}
