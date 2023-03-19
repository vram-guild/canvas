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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

// WIP: Need to remove FAPI event dependency or make it soft
class SimpleDrawersHolder {
	static ItemModelHandler itemCallbackHandler = (stack, renderMode, leftHanded, model) -> model;
	private static boolean warnRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("simpledrawers")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("me.benfah.simpledrawers.callback.RedirectModelCallback");
				final Object instance = ((Event<?>) clazz.getDeclaredField("EVENT").get(null)).invoker();

				final Method onRender = clazz.getDeclaredMethod("onRender", ItemStack.class, ItemDisplayContext.class, boolean.class, BakedModel.class);
				final MethodHandle onRenderHandler = lookup.unreflect(onRender);
				final MethodHandle boundOnRenderHandler = onRenderHandler.bindTo(instance);

				itemCallbackHandler = (stack, renderMode, leftHanded, model) -> {
					try {
						return (BakedModel) boundOnRenderHandler.invokeExact(stack, renderMode, leftHanded, model);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call SimpleDrawers RedirectModelCallback.onRender hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}

					return model;
				};

				CanvasMod.LOG.info("Found Simple Drawers - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find Simple Drawers render hook due to exception:", e);
			}
		}
	}

	interface ItemModelHandler {
		BakedModel onRender(ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, BakedModel model);
	}
}
