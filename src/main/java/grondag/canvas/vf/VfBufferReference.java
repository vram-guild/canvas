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
import java.util.function.BooleanSupplier;

public class VfBufferReference implements VfBufferElement<VfBufferReference> {
	protected final int byteSize;
	protected final BooleanSupplier closeFunc;

	protected int byteAddress;
	protected BufferWriter writer;

	public VfBufferReference(BufferWriter writer, int byteSize, BooleanSupplier closeFunc) {
		this.writer = writer;
		this.byteSize = byteSize;
		this.closeFunc = closeFunc;

		assert writer != null;
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
	public void write(IntBuffer buff, int startIndex) {
		if (writer != null) {
			writer.write(buff, startIndex);
			writer = null;
		} else {
			assert false : "Buffer write attempted more than once";
		}
	}

	@Override
	public int byteSize() {
		return byteSize;
	}

	@Override
	public boolean isClosed() {
		return closeFunc.getAsBoolean();
	}

	public static VfBufferReference of(BufferWriter writer, int byteSize, BooleanSupplier closeFunc) {
		return new VfBufferReference(writer, byteSize, closeFunc);
	}
}
