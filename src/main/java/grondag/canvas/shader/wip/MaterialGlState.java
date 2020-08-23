package grondag.canvas.shader.wip;


public class MaterialGlState {

	public static class Builder {
		// WIP: texture binding
		// WIP: transparency
		// WIP: depth test
		// WIP: cull
		// WIP: enable lightmap

		private boolean sorted = false;

		public Builder sorted(boolean sorted) {
			this.sorted = sorted;
			return this;
		}
	}
}
