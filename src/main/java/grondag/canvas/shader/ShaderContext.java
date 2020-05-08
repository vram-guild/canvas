package grondag.canvas.shader;

import grondag.canvas.material.MaterialContext;

public class ShaderContext {
	private static int indexCounter;

	public final int index = ++indexCounter;
	public final String name;
	public final MaterialContext materialContext;
	public final int spriteDepth;
	public final boolean isCutout;
	public final boolean hdLightmaps;

	private ShaderContext(Builder builder) {
		name = builder.name;
		materialContext = builder.materialContext;
		spriteDepth = builder.spriteDepth;
		isCutout = builder.isCutout;
		hdLightmaps = builder.hdLightmaps;
	}

	public static class Builder {
		private String name;
		private MaterialContext materialContext;
		private int spriteDepth = 1;
		private boolean isCutout = false;
		private boolean hdLightmaps = false;

		Builder name(String name) {
			this.name = name;
			return this;
		}

		Builder materialContext(MaterialContext materialContext) {
			this.materialContext = materialContext;
			return this;
		}

		Builder spriteDepth(int spriteDepth) {
			this.spriteDepth = spriteDepth;
			return this;
		}

		// TODO: remove?
		Builder isCutout(boolean isCutout) {
			this.isCutout = isCutout;
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

	public static final ShaderContext VANILLA_TERRAIN = builder()
			.hdLightmaps(false)
			.isCutout(false)
			.materialContext(MaterialContext.TERRAIN)
			.build();

}
