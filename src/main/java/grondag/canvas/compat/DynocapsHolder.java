/*
 * Copyright © Original Authors
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

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class DynocapsHolder {
	static DynoCapsRender handler = (profiler, matrixStack, immediate, camPos) -> { };

	private static boolean warnRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("dynocaps")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("com.biom4st3r.dynocaps.features.BoxRenderer");
				final Method render = clazz.getDeclaredMethod("render", PoseStack.class, MultiBufferSource.BufferSource.class, int.class, Vec3.class);
				final MethodHandle renderHandler = lookup.unreflect(render);

				handler = (profiler, matrixStack, immediate, camPos) -> {
					try {
						profiler.popPush("dynocaps");
						renderHandler.invokeExact(matrixStack, immediate, 1, camPos);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call DynoCaps BoxRenderer.render hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found DynoCaps - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find DynoCaps render hook due to exception:", e);
			}
		}
	}

	interface DynoCapsRender {
		void render(ProfilerFiller profiler, PoseStack matrixStack, MultiBufferSource.BufferSource immediate, Vec3 camPos);
	}
}
