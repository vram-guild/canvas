package grondag.canvas.shader;

import net.minecraft.util.Identifier;

import grondag.canvas.material.MaterialVertexFormats;

public enum EntityShader {
	DEFAULT_SOLID(ShaderData.DEFAULT_ENTITY_VERTEX, ShaderData.DEFAULT_ENTITY_FRAGMENT, ShaderContext.ENTITY_BLOCK_SOLID);

	private GlProgram program;

	private final Identifier fragmentId;
	private final Identifier vertexId;
	private final ShaderContext context;

	EntityShader(Identifier vertexId, Identifier fragmentId, ShaderContext context) {
		this.fragmentId = fragmentId;
		this.vertexId = vertexId;
		this.context = context;
	}

	void unload() {
		if (program != null) {
			program.unload();
			program = null;
		}
	}

	public EntityShader activate() {
		if (program == null) {
			final GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexId, context);
			final GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentId, context);
			program = new GlProgram(vs, fs, MaterialVertexFormats.TEMPORARY_ENTITY_FORMAT, context);
			ShaderData.STANDARD_UNIFORM_SETUP.accept(program);
			program.load();
		}

		program.activate();

		return this;
	}

	public static void reload() {
		for  (final EntityShader s : values()) {
			s.unload();
		}
	}
}
