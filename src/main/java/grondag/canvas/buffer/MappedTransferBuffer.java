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

package grondag.canvas.buffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

public class MappedTransferBuffer implements TransferBuffer {
	@Override
	public boolean isMappedBuffer() {
		return true;
	}

	@Override
	public @Nullable TransferBuffer releaseToMappedBuffer(ByteBuffer targetBuffer, int targetOffset, int sourceOffset, int byteCount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntBuffer asIntBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable TransferBuffer release() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToBuffer(int target, int usage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToSubBuffer(int glArrayBuffer, int unpackVacancyAddress, int vacantBytes) {
		// TODO Auto-generated method stub
		return null;
	}
}
