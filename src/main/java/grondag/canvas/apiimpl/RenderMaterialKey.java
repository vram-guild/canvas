package grondag.canvas.apiimpl;

class RenderMaterialKey {
	protected long bits0;
	protected long bits1;

	RenderMaterialKey(long bits0, long bits1) {
		this.bits0 = bits0;
		this.bits1 = bits1;
	}

	@Override
	public boolean equals(Object other) {
		if(other == null || !(other instanceof RenderMaterialKey)) {
			return false;
		}

		final RenderMaterialKey o = (RenderMaterialKey) other;

		return bits0 == o.bits0 && bits1 == o.bits1;
	}

	@Override
	public int hashCode() {
		return it.unimi.dsi.fastutil.HashCommon.long2int(bits0) * 31 + it.unimi.dsi.fastutil.HashCommon.long2int(bits1);
	}
}
