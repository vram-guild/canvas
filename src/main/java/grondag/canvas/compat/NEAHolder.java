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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

/**
 * Not Enough Animations compatibility.
 * Exists due to level renderer mixin conflict.
 */
public class NEAHolder {
	private static Runnable neaHolder = () -> { };

	static {
		if (FabricLoader.getInstance().isModLoaded("notenoughanimations")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> loaderClass = Class.forName("dev.tr7zw.notenoughanimations.NEAnimationsLoader");

				final Field instanceHook = loaderClass.getDeclaredField("INSTANCE");
				// final MethodHandle instanceHandler = lookup.unreflectGetter(instanceHook);
				final Object instance = instanceHook.get(null);

				final Field playerTransformerHook = loaderClass.getDeclaredField("playerTransformer");
				final MethodHandle playerTransformerHandler = lookup.unreflectGetter(playerTransformerHook);
				final Object playerTransformer = playerTransformerHandler.bindTo(instance).invoke();

				final Class<?> playerTransformerClass = Class.forName("dev.tr7zw.notenoughanimations.logic.PlayerTransformer");

				final Method nextFrameHook = playerTransformerClass.getDeclaredMethod("nextFrame");
				final MethodHandle nextFrameHandler = lookup.unreflect(nextFrameHook);
				final MethodHandle boundNextFrameHandler = nextFrameHandler.bindTo(playerTransformer);

				neaHolder = () -> {
					try {
						boundNextFrameHandler.invokeExact();
						// Commented out for now because it hasn't broken yet
						// nextFrameHandler.bindTo(playerTransformerHandler.bindTo(instanceHandler.invoke()).invoke()).invoke();
					} catch (final Throwable e) {
						// Commented out for now because it hasn't broken yet
						// if (warnRender) {
						// 	CanvasMod.LOG.warn("", e);
						// 	CanvasMod.LOG.warn("Subsequent errors will be suppressed");
						// 	warnRender = false;
						// }
					}
				};

				CanvasMod.LOG.info("Not Enough Animations compatibility hook is enabled.");
			} catch (final Throwable e) {
				CanvasMod.LOG.warn("Failed to initialize Not Enough Animations compatibility: ", e);
			}
		}
	}

	public static void worldRenderStart() {
		neaHolder.run();
	}
}
