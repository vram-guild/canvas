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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class JustMapHolder {
	static JustMapRender justMapRender = (matrixStack, camera, tickDelta) -> { };
	private static boolean warnRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("justmap")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("ru.bulldog.justmap.client.render.WaypointRenderer");
				final Method render = clazz.getDeclaredMethod("renderWaypoints", PoseStack.class, Camera.class, float.class);
				final MethodHandle renderHandler = lookup.unreflect(render);

				justMapRender = (matrixStack, camera, tickDelta) -> {
					try {
						renderHandler.invokeExact(matrixStack, camera, tickDelta);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call Just Map renderWaypoints hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found Just Map - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find Just Map render hook due to exception:", e);
			}
		}
	}

	interface JustMapRender {
		void renderWaypoints(PoseStack matrixStack, Camera camera, float tickDelta);
	}
}
