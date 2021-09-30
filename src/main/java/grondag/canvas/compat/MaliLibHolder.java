/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
