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

import com.google.common.util.concurrent.Runnables;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.fabricmc.loader.api.FabricLoader;

import io.vram.frex.api.renderloop.WorldRenderContext;

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
