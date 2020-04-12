package grondag.canvas.chunk.occlusion;

/**
 * Edge classifications - refers to position in the triangle.
 * Pixels above top edge, for example, are outside the edge.
 */
public enum EdgePosition {
	TOP(false, false, true, false) {
		@Override
		public long shiftMask(long mask) {
			return (mask >>> 8);
		}
	},

	BOTTOM(false, false, false, true) {
		@Override
		public long shiftMask(long mask) {
			return mask << 8;
		}
	},

	LEFT(true, false, false, false) {
		@Override
		public long shiftMask(long mask) {
			return (mask << 1) & 0xFEFEFEFEFEFEFEFEL;
		}
	},

	RIGHT(false, true, false, false) {
		@Override
		public long shiftMask(long mask) {
			return (mask >>> 1) & 0x7F7F7F7F7F7F7F7FL;
		}
	},

	TOP_LEFT(true, false, true, false) {
		@Override
		public long shiftMask(long mask) {
			return (((mask << 1) & 0xFEFEFEFEFEFEFEFEL) >>> 8);
		}
	},

	TOP_RIGHT(false, true, true, false) {
		@Override
		public long shiftMask(long mask) {
			return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) >>> 8;
		}
	},

	BOTTOM_LEFT(true, false, false, true) {
		@Override
		public long shiftMask(long mask) {
			return ((mask << 1) & 0xFEFEFEFEFEFEFEFEL) << 8;
		}
	},

	BOTTOM_RIGHT(false, true, false, true) {
		@Override
		public long shiftMask(long mask) {
			return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) << 8;
		}
	};

	//	public abstract int blort();
	public final boolean isRight;
	public final boolean isLeft;
	public final boolean isTop;
	public final boolean isBottom;

	private EdgePosition(boolean isLeft, boolean isRight, boolean isTop, boolean isBottom) {
		this.isRight = isRight;
		this.isLeft = isLeft;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	/**
	 * Shifts mask 1 pixel towards positive half plane
	 * Use to construct full coverage masks
	 * @param mask
	 * @param edgeFlag
	 * @return
	 */
	public abstract long shiftMask(long mask);
}
