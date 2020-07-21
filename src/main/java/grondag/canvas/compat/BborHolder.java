package grondag.canvas.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.loader.api.FabricLoader;

import grondag.canvas.CanvasMod;

public class BborHolder {
	private static boolean warnRender = true;

	public static RenderHandler bborHandler = (m, t, p) -> {};

	static {
		if (FabricLoader.getInstance().isModLoaded("bbor")) {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();

			try {
				final Class<?> clazz = Class.forName("com.irtimaled.bbor.client.interop.ClientInterop");

				//				for (final Method m : clazz.getDeclaredMethods()) {
				//					CanvasMod.LOG.info(m.toString());
				//				}


				final Method renderHook = clazz.getDeclaredMethod("render", MatrixStack.class, float.class, ClientPlayerEntity.class);
				final MethodHandle renderHookHandler = lookup.unreflect(renderHook);

				bborHandler = (m, t, p) -> {
					try  {
						renderHookHandler.invokeExact(m, t, p);
					} catch (final Throwable e) {
						if (warnRender) {
							CanvasMod.LOG.warn("Unable to call Bounding Box Outline Reloaded render hook due to exception:", e);
							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
							warnRender = false;
						}
					}
				};

				CanvasMod.LOG.info("Found Bounding Box Outline Reloaded - compatibility hook enabled");
			} catch (final Exception e)  {
				CanvasMod.LOG.warn("Unable to find Bounding Box Outline Reloaded render hook due to exception:", e);
			}
		}
	}

	public interface RenderHandler {
		void render(MatrixStack matrixStack, float partialTicks, ClientPlayerEntity player);
	}
}
