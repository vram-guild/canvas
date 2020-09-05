package grondag.canvas.light;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.Configurator;

@Environment(EnvType.CLIENT)
public final class AoVertexClampFunction {
	@FunctionalInterface
	private interface ClampFunc {
		float clamp(float x);
	}

	static ClampFunc func;

	static {
		reload();
	}

	public static void reload() {
		func = Configurator.clampExteriorVertices ? x -> x < 0f ? 0f : (x > 1f ? 1f : x) : x -> x;
	}

	static float clamp(float x) {
		return func.clamp(x);
	}
}
