package grondag.canvas.pipeline;

import net.minecraft.util.Identifier;

import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

public class ProcessShaders {
	private static final SimpleUnorderedArrayList<ProcessShader> ALL = new SimpleUnorderedArrayList<>();

	public static ProcessShader create(String baseName, String... samplers) {
		final ProcessShader result = new ProcessShader(new Identifier(baseName + ".vert"), new Identifier(baseName + ".frag"), samplers);
		ALL.add(result);
		return result;
	}

	public static void reload() {
		for (final ProcessShader shader :  ALL) {
			shader.unload();
		}
	}
}
