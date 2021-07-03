package grondag.canvas.vf;

import java.nio.IntBuffer;

class IntElement implements VfElement<IntElement> {
	private int c0, c1, c2, c3, hashCode;
	int index;

	void set(int c0, int c1, int c2, int c3) {
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		hashCode = VfTexture.hash4(c0, c1, c2, c3);
	}

	@Override
	public IntElement copy() {
		IntElement result = new IntElement();
		result.set(c0, c1, c2, c3);
		return result;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public void write(IntBuffer buff, final int startIndex) {
		buff.put(startIndex, c0);
		buff.put(startIndex + 1, c1);
		buff.put(startIndex + 2, c2);
		buff.put(startIndex + 3, c3);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntElement) {
			final IntElement k = (IntElement) obj;
			return k.c0 == c0 && k.c1 == c1 && k.c2 == c2 && k.c3 == c3;
		} else {
			return false;
		}
	}
}
