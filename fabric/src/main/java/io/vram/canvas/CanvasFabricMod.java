/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.vram.canvas;

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
import grondag.canvas.light.color.LightRegistry;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.texture.MaterialIndexProvider;

public class CanvasFabricMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CanvasMod.versionString = FabricLoader.getInstance().getModContainer(CanvasMod.MODID).get().getMetadata().getVersion().getFriendlyString();

		CanvasMod.init();

		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_TOGGLE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_PREV);
		KeyBindingHelper.registerKeyBinding(CanvasMod.DEBUG_NEXT);
		KeyBindingHelper.registerKeyBinding(CanvasMod.RECOMPILE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.FLAWLESS_TOGGLE);
		KeyBindingHelper.registerKeyBinding(CanvasMod.PROFILER_TOGGLE);

		FabricLoader.getInstance().getModContainer(CanvasMod.MODID).ifPresent(modContainer -> {
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:abstract"), modContainer, ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(new ResourceLocation("canvas:canvas_default"), modContainer, ResourcePackActivationType.NORMAL);
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
				LightRegistry.reload(manager);
			}
		});
	}

	private static final ResourceLocation ID = new ResourceLocation("canvas:resource_reloader");
}
