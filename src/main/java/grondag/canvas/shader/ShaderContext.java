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

	public final boolean hdLightmaps;

	private ShaderContext(Builder builder) {
		name = builder.name;
		materialContext = builder.materialContext;
		type = builder.type;
		hdLightmaps = builder.hdLightmaps;
	}

	public static class Builder {
		private String name;
		private MaterialContext materialContext;
		private Type type = Type.SOLID;
		private boolean hdLightmaps = false;

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

		Builder hdLightmaps(boolean hdLightmaps) {
			this.hdLightmaps = hdLightmaps;
			return this;
		}

		ShaderContext build() {
			return new ShaderContext(this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final ShaderContext VANILLA_TERRAIN_SOLID = builder()
			.hdLightmaps(false)
			.type(Type.SOLID)
			.materialContext(MaterialContext.TERRAIN)
			.build();

	public static final ShaderContext VANILLA_TERRAIN_DECAL = builder()
			.hdLightmaps(false)
			.type(Type.DECAL)
			.materialContext(MaterialContext.TERRAIN)
			.build();

	public static final ShaderContext VANILLA_TERRAIN_TRANSLUCENT = builder()
			.hdLightmaps(false)
			.type(Type.TRANSLUCENT)
			.materialContext(MaterialContext.TERRAIN)
			.build();
}
