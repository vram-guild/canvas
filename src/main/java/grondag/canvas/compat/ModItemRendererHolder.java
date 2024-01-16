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

package grondag.canvas.compat;

import java.lang.reflect.Method;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class ModItemRendererHolder {
	@FunctionalInterface
	public interface GetterFunc {
		BlockEntityWithoutLevelRenderer apply(ItemStack itemStack, BlockEntityWithoutLevelRenderer builtIn);
	}

	private static final GetterFunc NULL = (i, b) -> null;
	private static GetterFunc azureGetter = NULL;
	private static GetterFunc geckoGetter = NULL;

	public static GetterFunc rendererGetter = (i, b) -> {
		// azure getter is prioritized because it has an extra check
		var renderer = azureGetter.apply(i, b);

		if (renderer == null) {
			renderer = geckoGetter.apply(i, b);
		}

		return renderer;
	};

	static {
		if (FabricLoader.getInstance().isModLoaded("geckolib")) {
			geckoLibInit();
		}

		if (FabricLoader.getInstance().isModLoaded("azurelib")) {
			azureLibInit();
		}
	}

	private static void geckoLibInit() {
		final String modName = "GeckoLib";

		try {
			final Class<?> geckolibProvider = Class.forName("software.bernie.geckolib.animatable.client.RenderProvider");
			final Method geckoOf = geckolibProvider.getDeclaredMethod("of", ItemStack.class);
			final Method geckoGetCustomProvider = geckolibProvider.getDeclaredMethod("getCustomRenderer");

			geckoGetter = (itemStack, builtIn) -> {
				try {
					// final BlockEntityWithoutLevelRenderer renderer = RenderProvider.of(itemStack).getCustomRenderer();
					final Object renderProvider = geckoOf.invoke(null, itemStack);
					final var customRenderer = (BlockEntityWithoutLevelRenderer) geckoGetCustomProvider.invoke(renderProvider);

					if (customRenderer == builtIn) {
						return null;
					} else {
						return customRenderer;
					}
				} catch (Throwable ignored) {
					geckoGetter = NULL;
					onCrashing(modName);
				}

				return null;
			};

			onInit(modName);
		} catch (Throwable ignored) {
			onInitFail(modName);
		}
	}

	private static void azureLibInit() {
		final String modName = "AzureLib";

		try {
			final Class<?> azurelibGeoItem = Class.forName("mod.azure.azurelib.animatable.GeoItem");
			final Class<?> azurelibProvider = Class.forName("mod.azure.azurelib.animatable.client.RenderProvider");
			final Method azureOf = azurelibProvider.getDeclaredMethod("of", ItemStack.class);
			final Method azureGetCustomRenderer = azurelibProvider.getDeclaredMethod("getCustomRenderer");

			azureGetter = (itemStack, builtIn) -> {
				try {
					if (azurelibGeoItem.isInstance(itemStack.getItem())) {
						// RenderProvider.of(itemStack).getCustomRenderer().renderByItem(itemStack, transformType, poseStack, multiBufferSource, light, overlay);
						final Object renderProvider = azureOf.invoke(null, itemStack);
						final var customRenderer = (BlockEntityWithoutLevelRenderer) azureGetCustomRenderer.invoke(renderProvider);

						if (customRenderer == builtIn) {
							return null;
						} else {
							return customRenderer;
						}
					}
				} catch (Throwable ignored) {
					azureGetter = NULL;
					onCrashing(modName);
				}

				return null;
			};

			onInit(modName);
		} catch (Throwable ignored) {
			onInitFail(modName);
		}
	}

	private static void onCrashing(String modName) {
		CanvasMod.LOG.warn(modName + " compatibility hook crashed. Disabling it.");

		if (azureGetter == NULL && geckoGetter == NULL) {
			rendererGetter = NULL;
		}
	}

	private static void onInit(String modName) {
		CanvasMod.LOG.warn(modName + " compatibility hook initialized successfully.");
	}

	private static void onInitFail(String modName) {
		CanvasMod.LOG.warn(modName + " compatibility hook initialization failed.");
	}
}
