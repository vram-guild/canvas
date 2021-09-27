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

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.vertex.PoseStack;
import io.vram.frex.api.renderloop.WorldRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class BborHolder {
	static RenderHandler bborHandler = (m, t, p) -> { };
	static Runnable bborDeferredHandler = Runnables.doNothing();

	private static boolean warnRender = true;
	private static boolean warnDeferredRender = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("bbor")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("com.irtimaled.bbor.client.interop.ClientInterop");

				final Method renderHook = clazz.getDeclaredMethod("render", PoseStack.class, float.class, LocalPlayer.class);
				final MethodHandle renderHookHandler = lookup.unreflect(renderHook);

				final Method deferredHook = clazz.getDeclaredMethod("renderDeferred");
				final MethodHandle deferredHookHandler = lookup.unreflect(deferredHook);

				bborHandler = (m, t, p) -> {
					try {
						renderHookHandler.invokeExact(m, t, p);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call Bounding Box Outline Reloaded render hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				bborDeferredHandler = () -> {
					try {
						deferredHookHandler.invokeExact();
					} catch (final Throwable e) {
						if (warnDeferredRender) {
							CanvasMod.LOG.warn("Unable to call Bounding Box Outline Reloaded deferred render hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnDeferredRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found Bounding Box Outline Reloaded - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find Bounding Box Outline Reloaded render hook due to exception:", e);
			}
		}
	}

	@SuppressWarnings("resource")
	static void render(WorldRenderContext ctx) {
		bborHandler.render(ctx.poseStack(), ctx.tickDelta(), Minecraft.getInstance().player);
	}

	interface RenderHandler {
		void render(PoseStack matrixStack, float partialTicks, LocalPlayer player);
	}

	static void deferred() {
		bborDeferredHandler.run();
	}
}
