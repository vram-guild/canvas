package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class JustMapHolder {
	private static boolean warnRender = true;

	public static JustMapRender justMapRender = (matrixStack, client, camera, tickDelta) -> {};

	static {
		if (FabricLoader.getInstance().isModLoaded("justmap")) {

			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("ru.bulldog.justmap.client.render.WaypointRenderer");
				final Method render = clazz.getDeclaredMethod("renderWaypoints", MatrixStack.class, MinecraftClient.class, Camera.class, float.class);
				final MethodHandle renderHandler = lookup.unreflect(render);

				justMapRender = (matrixStack, client, camera, tickDelta) -> {
					try  {
						renderHandler.invokeExact(matrixStack, client, camera, tickDelta);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call Just Map renderWaypoints hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found Just Map - compatibility hook enabled");
			} catch (final Exception e)  {
				CanvasMod.LOG.warn("Unable to find Just Map render hook due to exception:", e);
			}
		}
	}

	public interface JustMapRender {
		void renderWaypoints(MatrixStack matrixStack, MinecraftClient client, Camera camera, float tickDelta);
	}
}
