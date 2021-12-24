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
import java.util.function.Consumer;

import net.minecraft.client.renderer.LevelRenderer;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class LambDynLightsHolder {
	static Consumer<LevelRenderer> updateAll = wr -> { };
	private static boolean warnInit = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("lambdynlights")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("me.lambdaurora.lambdynlights.LambDynLights");
				final Method getInstance = clazz.getDeclaredMethod("get");
				final Object instance = getInstance.invoke(null);

				final Method update = clazz.getDeclaredMethod("updateAll", LevelRenderer.class);
				final MethodHandle updateHandler = lookup.unreflect(update);
				final MethodHandle boundReUpdateHandler = updateHandler.bindTo(instance);

				updateAll = wr -> {
					try {
						boundReUpdateHandler.invokeExact(wr);
					} catch (final Throwable e) {
						if (warnInit) {
							CanvasMod.LOG.warn("Unable to call LambDynLights updateAll hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnInit = false;
						}
					}
				};

				CanvasMod.LOG.info("Found LambDynLights - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find LambDynLights render hook due to exception:", e);
			}
		}
	}
}
