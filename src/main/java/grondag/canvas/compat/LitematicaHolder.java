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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.util.concurrent.Runnables;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

class LitematicaHolder {
	static Runnable litematicaReload = Runnables.doNothing();
	static Consumer<Frustum> litematicaTerrainSetup = f -> { };
	static BiConsumer<MatrixStack, Matrix4f> litematicaRenderSolids = (s, p) -> { };
	static BiConsumer<MatrixStack, Matrix4f> litematicaRenderTranslucent = (s, p) -> { };
	static BiConsumer<MatrixStack, Matrix4f> litematicaRenderOverlay = (s, p) -> { };
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

				final Method solid = clazz.getDeclaredMethod("piecewiseRenderSolid", MatrixStack.class, Matrix4f.class);
				final MethodHandle solidHandler = lookup.unreflect(solid);
				final MethodHandle boundSolidHandler = solidHandler.bindTo(instance);

				final Method cutout = clazz.getDeclaredMethod("piecewiseRenderCutout", MatrixStack.class, Matrix4f.class);
				final MethodHandle cutoutHandler = lookup.unreflect(cutout);
				final MethodHandle boundCutoutHandler = cutoutHandler.bindTo(instance);

				final Method mipped = clazz.getDeclaredMethod("piecewiseRenderCutoutMipped", MatrixStack.class, Matrix4f.class);
				final MethodHandle mippedHandler = lookup.unreflect(mipped);
				final MethodHandle boundMippedHandler = mippedHandler.bindTo(instance);

				final Method translucent = clazz.getDeclaredMethod("piecewiseRenderTranslucent", MatrixStack.class, Matrix4f.class);
				final MethodHandle translucentHandler = lookup.unreflect(translucent);
				final MethodHandle boundTranslucentHandler = translucentHandler.bindTo(instance);

				final Method overlay = clazz.getDeclaredMethod("piecewiseRenderOverlay", MatrixStack.class, Matrix4f.class);
				final MethodHandle overlayHandler = lookup.unreflect(overlay);
				final MethodHandle boundOverlayHandler = overlayHandler.bindTo(instance);

				final Method entities = clazz.getDeclaredMethod("piecewiseRenderEntities", MatrixStack.class, float.class);
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
		void handle(MatrixStack matrices, float partialTicks);
	}
}
