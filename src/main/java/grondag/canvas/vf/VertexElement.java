package grondag.canvas.vf;

import java.nio.IntBuffer;
import java.util.Arrays;

class VertexElement implements VfElement<VertexElement> {
	final int[] data = new int[16];
	private int hashCode;
	int index;

	void compute() {
		hashCode = Arrays.hashCode(data);
	}

	@Override
	public VertexElement copy() {
		VertexElement result = new VertexElement();
		System.arraycopy(data, 0, result.data, 0, 16);
		result.hashCode = hashCode;
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
		buff.put(startIndex, data);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VertexElement) {
			return Arrays.equals(data, ((VertexElement) obj).data);
		} else {
			return false;
		}
	}
}
