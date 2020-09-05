package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import net.minecraft.client.render.WorldRenderer;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class LambDynLightsHolder {
	private static boolean warnInit = true;

	public static Consumer<WorldRenderer> updateAll = wr -> {};

	static {
		if (FabricLoader.getInstance().isModLoaded("lambdynlights")) {

			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("me.lambdaurora.lambdynlights.LambDynLights");
				final Method getInstance = clazz.getDeclaredMethod("get");
				final Object instance =  getInstance.invoke(null);

				final Method update = clazz.getDeclaredMethod("updateAll", WorldRenderer.class);
				final MethodHandle updateHandler = lookup.unreflect(update);
				final MethodHandle boundReUpdateHandler = updateHandler.bindTo(instance);

				updateAll = wr -> {
					try  {
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
			} catch (final Exception e)  {
				CanvasMod.LOG.warn("Unable to find LambDynLights render hook due to exception:", e);
			}
		}
	}
}
