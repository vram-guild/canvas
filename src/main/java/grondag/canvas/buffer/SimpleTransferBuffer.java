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

import net.minecraft.util.math.MathHelper;

import grondag.canvas.buffer.SimpleTransferBufferAllocator.AllocationState;
import grondag.canvas.buffer.util.DirectBufferAllocator;
import grondag.canvas.buffer.util.DirectBufferAllocator.DirectBufferReference;
import grondag.canvas.varia.GFX;

class SimpleTransferBuffer implements TransferBuffer {
	private DirectBufferReference bufferRef;
	final int capacityBytes;
	final AllocationState allocationState;

	SimpleTransferBuffer(int bytes, AllocationState allocationState) {
		this.allocationState = allocationState;

		if (bytes < 4096) {
			bytes = 4096;
		}

		capacityBytes = MathHelper.smallestEncompassingPowerOfTwo(bytes);
		bufferRef = DirectBufferAllocator.claim(capacityBytes);
		allocationState.add(this);
	}

	@Override
	public IntBuffer asIntBuffer() {
		return bufferRef.buffer().asIntBuffer();
	}

	@Override
	public @Nullable TransferBuffer release() {
		releaseWithoutNotify();
		allocationState.remove(this);
		return null;
	}

	void releaseWithoutNotify() {
		assert bufferRef != null;

		if (bufferRef != null) {
			bufferRef.release();
			bufferRef = null;
		}
	}

	@Override
	public @Nullable TransferBuffer releaseToMappedBuffer(ByteBuffer targetBuffer, int targetOffset, int sourceOffset, int byteCount) {
		targetBuffer.put(targetOffset, bufferRef.buffer(), sourceOffset, byteCount);
		release();
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToBuffer(int target, int usage) {
		GFX.bufferData(target, bufferRef.buffer(), usage);
		release();
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToSubBuffer(int target, int addressBytes, int lenBytes) {
		GFX.bufferSubData(target, addressBytes, lenBytes, bufferRef.buffer());
		release();
		return null;
	}
}
