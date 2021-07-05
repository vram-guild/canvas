/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.vf;

import java.nio.IntBuffer;

public abstract class VfBufferReference implements VfBufferElement<VfBufferReference>, AutoCloseable {
	protected final int byteSize;
	protected boolean isClosed = false;

	protected int byteAddress;

	public VfBufferReference(int byteSize) {
		this.byteSize = byteSize;

		assert byteSize != 0;
		assert (byteSize & 3) == 0 : "Buffer size must be int-aligned";
	}

	@Override
	public void setByteAddress(int address) {
		byteAddress = address;
	}

	@Override
	public int getByteAddress() {
		return byteAddress;
	}

	@Override
	public int byteSize() {
		return byteSize;
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() {
		isClosed = true;
	}

	private static class VfArrayBackedBufferReference extends VfBufferReference {
		int[] data;

		VfArrayBackedBufferReference(int[] data) {
			super(data.length * 4);
			this.data = data;
		}

		@Override
		public void write(IntBuffer buff, int startIndex) {
			if (data != null) {
				buff.put(startIndex, data);
				data = null;
			} else {
				assert false : "Buffer write attempted more than once";
			}
		}
	}

	private static class VfWriterBackedBufferReference extends VfBufferReference {
		protected BufferWriter writer;

		VfWriterBackedBufferReference(int byteSize, BufferWriter writer) {
			super(byteSize);
			this.writer = writer;

			assert writer != null;
		}

		@Override
		public void write(IntBuffer buff, int startIndex) {
			if (writer != null) {
				writer.write(buff, startIndex);
				writer = null;
			} else {
				assert false : "Buffer write attempted more than once";
			}
		}
	}

	public static VfBufferReference of(int byteSize, BufferWriter writer) {
		return new VfWriterBackedBufferReference(byteSize, writer);
	}

	public static VfBufferReference of(int[] data) {
		return new VfArrayBackedBufferReference(data);
	}
}
