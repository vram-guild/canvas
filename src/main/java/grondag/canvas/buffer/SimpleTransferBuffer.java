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
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.buffer.SimpleTransferBufferAllocator.AllocationState;
import grondag.canvas.config.Configurator;
import grondag.canvas.varia.GFX;

class SimpleTransferBuffer implements TransferBuffer {
	private static final IntFunction<ByteBuffer> SUPPLIER = Configurator.safeNativeMemoryAllocation ? BufferUtils::createByteBuffer : MemoryUtil::memAlloc;
	private static final Consumer<ByteBuffer> CONSUMER = Configurator.safeNativeMemoryAllocation ? b -> { } : MemoryUtil::memFree;

	private ByteBuffer buffer;
	final int capacityBytes;
	final AllocationState allocationState;

	SimpleTransferBuffer(int bytes, AllocationState allocationState) {
		this.allocationState = allocationState;

		if (bytes < 4096) {
			bytes = 4096;
		}

		capacityBytes = MathHelper.smallestEncompassingPowerOfTwo(bytes);
		buffer = SUPPLIER.apply(capacityBytes);
		allocationState.add(this);
	}

	@Override
	public boolean isBufferCopySupported() {
		return false;
	}

	@Override
	public IntBuffer asIntBuffer() {
		return buffer.asIntBuffer();
	}

	@Override
	public @Nullable TransferBuffer release() {
		releaseWithoutNotify();
		allocationState.remove(this);
		return null;
	}

	void releaseWithoutNotify() {
		assert buffer != null;

		if (buffer != null) {
			CONSUMER.accept(buffer);
			buffer = null;
		}
	}

	@Override
	public @Nullable TransferBuffer releaseToSubBuffer(ByteBuffer targetBuffer, int targetOffset, int sourceOffset, int byteCount) {
		targetBuffer.put(targetOffset, buffer, sourceOffset, byteCount);
		release();
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToBuffer(int target, int usage) {
		GFX.bufferData(target, buffer, usage);
		release();
		return null;
	}

	@Override
	public @Nullable TransferBuffer releaseToSubBuffer(int target, int addressBytes, int lenBytes) {
		GFX.bufferSubData(target, addressBytes, lenBytes, buffer);
		release();
		return null;
	}
}
