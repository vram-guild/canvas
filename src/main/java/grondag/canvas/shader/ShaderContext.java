package grondag.canvas.shader;

import grondag.canvas.material.EncodingContext;

public class ShaderContext {
	private static int indexCounter;

	public final int index = ++indexCounter;

	public final EncodingContext materialContext;

	public final ShaderPass pass;

	public final String name;

	private ShaderContext(Builder builder) {
		materialContext = builder.materialContext;
		pass = builder.pass;
		name = materialContext == EncodingContext.PROCESS && pass == ShaderPass.PROCESS ? "process" : materialContext.name().toLowerCase() + "-" + pass.name().toLowerCase();
	}

	public static class Builder {
		private EncodingContext materialContext;
		private ShaderPass pass = ShaderPass.SOLID;

		Builder materialContext(EncodingContext materialContext) {
			this.materialContext = materialContext;
			return this;
		}

		Builder pass(ShaderPass pass) {
			this.pass = pass == null ? ShaderPass.SOLID : pass;
			return this;
		}

		ShaderContext build() {
			return new ShaderContext(this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final ShaderContext TERRAIN_SOLID = builder()
			.pass(ShaderPass.SOLID)
			.materialContext(EncodingContext.TERRAIN)
			.build();

	public static final ShaderContext TERRAIN_DECAL = builder()
			.pass(ShaderPass.DECAL)
			.materialContext(EncodingContext.TERRAIN)
			.build();

	public static final ShaderContext TERRAIN_TRANSLUCENT = builder()
			.pass(ShaderPass.TRANSLUCENT)
			.materialContext(EncodingContext.TERRAIN)
			.build();

	public static final ShaderContext PROCESS = builder()
			.pass(ShaderPass.PROCESS)
			.materialContext(EncodingContext.PROCESS)
			.build();

	public static final ShaderContext ENTITY_BLOCK_SOLID = builder()
			.pass(ShaderPass.SOLID)
			.materialContext(EncodingContext.ENTITY_BLOCK)
			.build();
}
