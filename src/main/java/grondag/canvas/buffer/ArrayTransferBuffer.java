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

import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

class ArrayTransferBuffer implements NewTransferBuffer {
	final int capacityBytes;
	final int[] data;
	int claimedBytes;

	ArrayTransferBuffer(int capacityBytes) {
		this.capacityBytes = capacityBytes;
		data = new int[capacityBytes];
	}

	@Override
	public @Nullable ArrayTransferBuffer release() {
		return null;
	}

	void claim(int claimedBytes) {
		this.claimedBytes = claimedBytes;
	}

	@Override
	public void put(int[] source, int sourceStartInts, int targetStartInts, int lengthInts) {
		assert sourceStartInts + lengthInts <= claimedBytes * 4;
		System.arraycopy(source, sourceStartInts, data, targetStartInts, lengthInts);
	}

	@Override
	public int sizeBytes() {
		return claimedBytes;
	}

	public @Nullable ArrayTransferBuffer releaseToMappedBuffer(IntBuffer targetBuffer, int targetStartBytes) {
		targetBuffer.put(targetStartBytes / 4, data, 0, claimedBytes / 4);
		release();
		return null;
	}

	@Override
	public @Nullable ArrayTransferBuffer releaseToBoundBuffer(int target, int targetStartBytes) {
		release();
		return null;
	}
}
