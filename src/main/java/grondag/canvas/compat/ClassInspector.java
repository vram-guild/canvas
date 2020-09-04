package grondag.canvas.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.render.WorldRenderer;

import grondag.canvas.CanvasMod;

public class ClassInspector {
	public static void inspect() {
		final Class<WorldRenderer> clazz = WorldRenderer.class;

		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("WorldRenderer Class Summary - For Developer Use");
		CanvasMod.LOG.info("=============================================");
		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("FIELDS");

		for(final Field f : clazz.getDeclaredFields()) {
			CanvasMod.LOG.info(f.toGenericString());
		}

		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("METHODS");

		for(final Method m : clazz.getDeclaredMethods()) {
			CanvasMod.LOG.info(m.toGenericString());
		}

		CanvasMod.LOG.info("");
	}
}
