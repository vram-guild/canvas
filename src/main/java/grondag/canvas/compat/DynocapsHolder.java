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
