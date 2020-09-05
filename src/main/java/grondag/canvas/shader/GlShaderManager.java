package grondag.canvas.shader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

public final class GlShaderManager {
	public final static GlShaderManager INSTANCE = new GlShaderManager();
	private final Object2ObjectOpenHashMap<String, GlShader> vertexShaders = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<String, GlShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

	public static String shaderKey(Identifier shaderSource, ShaderContext context) {
		return String.format("%s.%s", shaderSource.toString(), context.name);
	}

	public GlShader getOrCreateVertexShader(Identifier shaderSource, ShaderContext context) {
		final String shaderKey = shaderKey(shaderSource, context);

		synchronized (vertexShaders) {
			GlShader result = vertexShaders.get(shaderKey);
			if (result == null) {
				result = new GlShader(shaderSource, GL21.GL_VERTEX_SHADER, context);
				vertexShaders.put(shaderKey, result);
			}
			return result;
		}
	}

	public GlShader getOrCreateFragmentShader(Identifier shaderSourceId, ShaderContext context) {
		final String shaderKey = shaderKey(shaderSourceId, context);

		synchronized (fragmentShaders) {
			GlShader result = fragmentShaders.get(shaderKey);
			if (result == null) {
				result = new GlShader(shaderSourceId, GL21.GL_FRAGMENT_SHADER, context);
				fragmentShaders.put(shaderKey, result);
			}
			return result;
		}
	}

	public void reload() {
		GlShader.forceReloadErrors();
		fragmentShaders.values().forEach(s -> s.forceReload());
		vertexShaders.values().forEach(s -> s.forceReload());
	}
}
