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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class MaliLibHolder {
	static HandleRenderWorldLast maliLibRenderWorldLast = (s, mc, t) -> { };
	private static boolean warnRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("malilib")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("fi.dy.masa.malilib.event.RenderEventHandler");
				final Method getInstance = clazz.getDeclaredMethod("getInstance");
				final Object instance = getInstance.invoke(null);

				final Method renderLast = clazz.getDeclaredMethod("onRenderWorldLast", PoseStack.class, Matrix4f.class, Minecraft.class);
				final MethodHandle renderLastHandler = lookup.unreflect(renderLast);
				final MethodHandle boundRenderLastHandler = renderLastHandler.bindTo(instance);

				maliLibRenderWorldLast = (s, pm, mc) -> {
					try {
						boundRenderLastHandler.invokeExact(s, pm, mc);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call MaliLib onRenderWorldLast hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found MaliLib - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find MaliLib render hook due to exception:", e);
			}
		}
	}

	interface HandleRenderWorldLast {
		void render(PoseStack matrixStack, Matrix4f projMatrix, Minecraft mc);
	}
}
