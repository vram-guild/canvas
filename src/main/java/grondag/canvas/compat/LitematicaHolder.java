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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.util.concurrent.Runnables;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.culling.Frustum;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class LitematicaHolder {
	static Runnable litematicaReload = Runnables.doNothing();
	static Consumer<Frustum> litematicaTerrainSetup = f -> { };
	static BiConsumer<PoseStack, Matrix4f> litematicaRenderSolids = (s, p) -> { };
	static BiConsumer<PoseStack, Matrix4f> litematicaRenderTranslucent = (s, p) -> { };
	static BiConsumer<PoseStack, Matrix4f> litematicaRenderOverlay = (s, p) -> { };
	static EntityHandler litematicaEntityHandler = (s, t) -> { };

	private static boolean warnLoad = true;
	private static boolean warnPrepare = true;
	private static boolean warnSolid = true;
	private static boolean warnTranslucent = true;
	private static boolean warnEntities = true;
	private static boolean warnOverlay = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("litematica")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("fi.dy.masa.litematica.render.LitematicaRenderer");
				final Method getInstance = clazz.getDeclaredMethod("getInstance");
				final Object instance = getInstance.invoke(null);

				final Method reload = clazz.getDeclaredMethod("loadRenderers");
				final MethodHandle reloadHandler = lookup.unreflect(reload);
				final MethodHandle boundReloadHandler = reloadHandler.bindTo(instance);

				final Method prep = clazz.getDeclaredMethod("piecewisePrepareAndUpdate", Frustum.class);
				final MethodHandle prepHandler = lookup.unreflect(prep);
				final MethodHandle boundPrepHandler = prepHandler.bindTo(instance);

				final Method solid = clazz.getDeclaredMethod("piecewiseRenderSolid", PoseStack.class, Matrix4f.class);
				final MethodHandle solidHandler = lookup.unreflect(solid);
				final MethodHandle boundSolidHandler = solidHandler.bindTo(instance);

				final Method cutout = clazz.getDeclaredMethod("piecewiseRenderCutout", PoseStack.class, Matrix4f.class);
				final MethodHandle cutoutHandler = lookup.unreflect(cutout);
				final MethodHandle boundCutoutHandler = cutoutHandler.bindTo(instance);

				final Method mipped = clazz.getDeclaredMethod("piecewiseRenderCutoutMipped", PoseStack.class, Matrix4f.class);
				final MethodHandle mippedHandler = lookup.unreflect(mipped);
				final MethodHandle boundMippedHandler = mippedHandler.bindTo(instance);

				final Method translucent = clazz.getDeclaredMethod("piecewiseRenderTranslucent", PoseStack.class, Matrix4f.class);
				final MethodHandle translucentHandler = lookup.unreflect(translucent);
				final MethodHandle boundTranslucentHandler = translucentHandler.bindTo(instance);

				final Method overlay = clazz.getDeclaredMethod("piecewiseRenderOverlay", PoseStack.class, Matrix4f.class);
				final MethodHandle overlayHandler = lookup.unreflect(overlay);
				final MethodHandle boundOverlayHandler = overlayHandler.bindTo(instance);

				final Method entities = clazz.getDeclaredMethod("piecewiseRenderEntities", PoseStack.class, float.class);
				final MethodHandle entitiesHandler = lookup.unreflect(entities);
				final MethodHandle boundEntitiesHandler = entitiesHandler.bindTo(instance);

				litematicaReload = () -> {
					try {
						boundReloadHandler.invokeExact();
					} catch (final Throwable e) {
						if (warnLoad) {
							CanvasMod.LOG.warn("Unable to call Litematica reload hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnLoad = false;
						}
					}
				};

				litematicaTerrainSetup = (f) -> {
					try {
						boundPrepHandler.invokeExact(f);
					} catch (final Throwable e) {
						if (warnPrepare) {
							CanvasMod.LOG.warn("Unable to call Litematica prepare hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnPrepare = false;
						}
					}
				};

				litematicaRenderSolids = (s, p) -> {
					try {
						boundSolidHandler.invokeExact(s, p);
						boundCutoutHandler.invokeExact(s, p);
						boundMippedHandler.invokeExact(s, p);
					} catch (final Throwable e) {
						if (warnSolid) {
							CanvasMod.LOG.warn("Unable to call Litematica solids hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnSolid = false;
						}
					}
				};

				litematicaRenderTranslucent = (s, p) -> {
					try {
						boundTranslucentHandler.invokeExact(s, p);
					} catch (final Throwable e) {
						if (warnTranslucent) {
							CanvasMod.LOG.warn("Unable to call Litematica translucent hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnTranslucent = false;
						}
					}
				};

				litematicaRenderOverlay = (s, p) -> {
					try {
						boundOverlayHandler.invokeExact(s, p);
					} catch (final Throwable e) {
						if (warnOverlay) {
							CanvasMod.LOG.warn("Unable to call Litematica overlay hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnOverlay = false;
						}
					}
				};

				litematicaEntityHandler = (s, t) -> {
					try {
						boundEntitiesHandler.invokeExact(s, t);
					} catch (final Throwable e) {
						if (warnEntities) {
							CanvasMod.LOG.warn("Unable to call Litematica entity hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnEntities = false;
						}
					}
				};

				CanvasMod.LOG.info("Found Litematica - compatibility hook enabled");
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to find Litematica reload hook due to exception:", e);
			}
		}
	}

	interface EntityHandler {
		void handle(PoseStack matrices, float partialTicks);
	}
}
