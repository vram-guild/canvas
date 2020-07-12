package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class MaliLibHolder {
	private static boolean warnRender = true;


	static {
		if (FabricLoader.getInstance().isModLoaded("malilib")) {

			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("fi.dy.masa.malilib.event.RenderEventHandler");
				final Method getInstance = clazz.getDeclaredMethod("getInstance");
				final Object instance =  getInstance.invoke(null);

				final Method renderLast = clazz.getDeclaredMethod("onRenderWorldLast", MatrixStack.class, MinecraftClient.class, float.class);
				final MethodHandle renderLastHandler = lookup.unreflect(renderLast);
				final MethodHandle boundRenderLastHandler = renderLastHandler.bindTo(instance);

				litematicaRenderWorldLast = (s, mc, t) -> {
					try  {
						boundRenderLastHandler.invokeExact(s, mc, t);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call MaliLib onRenderWorldLast hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found MaliLib - compatibility hook enabled");
			} catch (final Exception e)  {
				CanvasMod.LOG.warn("Unable to find MaliLib render hook due to exception:", e);
			}
		}
	}

	public static HandleRenderWorldLast litematicaRenderWorldLast = (s, mc, t) -> {};

	public interface HandleRenderWorldLast {
		void render(MatrixStack matrixStack, MinecraftClient mc, float partialTicks);
	}
}
