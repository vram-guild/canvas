package grondag.canvas.shader;

import grondag.canvas.material.MaterialContext;

public class ShaderContext {
	public enum Type  {
		SOLID,
		DECAL,
		TRANSLUCENT
	}
	private static int indexCounter;

	public final int index = ++indexCounter;
	public final String name;

	public final MaterialContext materialContext;

	public final Type type;

	private ShaderContext(Builder builder) {
		name = builder.name;
		materialContext = builder.materialContext;
		type = builder.type;
	}

	public static class Builder {
		private String name;
		private MaterialContext materialContext;
		private Type type = Type.SOLID;

		Builder name(String name) {
			this.name = name;
			return this;
		}

		Builder materialContext(MaterialContext materialContext) {
			this.materialContext = materialContext;
			return this;
		}

		Builder type(Type type) {
			this.type = type == null ? Type.SOLID : type;
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
			.type(Type.SOLID)
			.materialContext(MaterialContext.TERRAIN)
			.build();

	public static final ShaderContext TERRAIN_DECAL = builder()
			.type(Type.DECAL)
			.materialContext(MaterialContext.TERRAIN)
			.build();

	public static final ShaderContext TERRAIN_TRANSLUCENT = builder()
			.type(Type.TRANSLUCENT)
			.materialContext(MaterialContext.TERRAIN)
			.build();
}
