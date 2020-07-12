package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import com.google.common.util.concurrent.Runnables;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class LitematicaHolder {
	private static boolean warnLoad = true;
	private static boolean warnPrepare = true;
	private static boolean warnSolid = true;
	private static boolean warnTranslucent = true;
	private static boolean warnEntities = true;

	static {
		if (FabricLoader.getInstance().isModLoaded("litematica")) {

			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("fi.dy.masa.litematica.render.LitematicaRenderer");
				final Method getInstance = clazz.getDeclaredMethod("getInstance");
				final Object instance =  getInstance.invoke(null);

				final Method reload = clazz.getDeclaredMethod("loadRenderers");
				final MethodHandle reloadHandler = lookup.unreflect(reload);
				final MethodHandle boundReloadHandler = reloadHandler.bindTo(instance);

				final Method prep = clazz.getDeclaredMethod("piecewisePrepareAndUpdate", Frustum.class);
				final MethodHandle prepHandler = lookup.unreflect(prep);
				final MethodHandle boundPrepHandler = prepHandler.bindTo(instance);

				final Method solid = clazz.getDeclaredMethod("piecewiseRenderSolid", MatrixStack.class);
				final MethodHandle solidHandler = lookup.unreflect(solid);
				final MethodHandle boundSolidHandler = solidHandler.bindTo(instance);

				final Method cutout = clazz.getDeclaredMethod("piecewiseRenderCutout", MatrixStack.class);
				final MethodHandle cutoutHandler = lookup.unreflect(cutout);
				final MethodHandle boundCutoutHandler = cutoutHandler.bindTo(instance);

				final Method mipped = clazz.getDeclaredMethod("piecewiseRenderCutoutMipped", MatrixStack.class);
				final MethodHandle mippedHandler = lookup.unreflect(mipped);
				final MethodHandle boundMippedHandler = mippedHandler.bindTo(instance);

				final Method translucent = clazz.getDeclaredMethod("piecewiseRenderTranslucent", MatrixStack.class);
				final MethodHandle translucentHandler = lookup.unreflect(translucent);
				final MethodHandle boundTranslucentHandler = translucentHandler.bindTo(instance);

				final Method entities = clazz.getDeclaredMethod("piecewiseRenderEntities", MatrixStack.class, float.class);
				final MethodHandle entitiesHandler = lookup.unreflect(entities);
				final MethodHandle boundEntitiesHandler = entitiesHandler.bindTo(instance);

				litematicaReload = () -> {
					try  {
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
					try  {
						boundPrepHandler.invokeExact(f);
					} catch (final Throwable e) {
						if (warnPrepare) {
							CanvasMod.LOG.warn("Unable to call Litematica prepare hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnPrepare = false;
						}
					}
				};

				litematicaRenderSolids = (s) -> {
					try  {
						boundSolidHandler.invokeExact(s);
						boundCutoutHandler.invokeExact(s);
						boundMippedHandler.invokeExact(s);
					} catch (final Throwable e) {
						if (warnSolid) {
							CanvasMod.LOG.warn("Unable to call Litematica solids hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnSolid = false;
						}
					}
				};

				litematicaRenderTranslucent = (s) -> {
					try  {
						boundTranslucentHandler.invokeExact(s);
					} catch (final Throwable e) {
						if (warnTranslucent) {
							CanvasMod.LOG.warn("Unable to call Litematica translucent hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnTranslucent = false;
						}
					}
				};

				litematicaEntityHandler = (s, t) -> {
					try  {
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
			} catch (final Exception e)  {
				CanvasMod.LOG.warn("Unable to find Litematica reload hook due to exception:", e);
			}
		}
	}

	public static Runnable litematicaReload = Runnables.doNothing();
	public static Consumer<Frustum> litematicaTerrainSetup = f -> {};
	public static Consumer<MatrixStack> litematicaRenderSolids = s -> {};
	public static Consumer<MatrixStack> litematicaRenderTranslucent = s -> {};
	public static EntityHandler litematicaEntityHandler = (s, t) -> {};

	public interface EntityHandler {
		void handle(MatrixStack matrices, float partialTicks);
	}
}
