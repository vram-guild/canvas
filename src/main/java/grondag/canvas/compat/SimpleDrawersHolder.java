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

package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class SimpleDrawersHolder {
	static ItemModelHandler itemCallbackHandler = (stack, renderMode, leftHanded, model) -> model;
	private static boolean warnRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("simpledrawers")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("me.benfah.simpledrawers.callback.RedirectModelCallback");
				final Object instance = ((Event<?>) clazz.getDeclaredField("EVENT").get(null)).invoker();

				final Method onRender = clazz.getDeclaredMethod("onRender", ItemStack.class, ModelTransformation.Mode.class, boolean.class, BakedModel.class);
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
		BakedModel onRender(ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, BakedModel model);
	}
}
