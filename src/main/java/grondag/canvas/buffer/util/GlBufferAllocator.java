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

package grondag.canvas.buffer.util;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.client.util.GlAllocationUtils;

import grondag.canvas.varia.GFX;

public class GlBufferAllocator {
	private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue(256);
	private static final IntBuffer buff = GlAllocationUtils.allocateByteBuffer(256 * 4).asIntBuffer();
	private static int allocatedCount = 0;
	private static int allocatedBytes = 0;

	public static int claimBuffer(int expectedBytes) {
		if (queue.isEmpty()) {
			// Buffer gen is slow on some Windows/NVidia systems so we buy in bulk
			GFX.genBuffers(buff);

			for (int i = 0; i < 256; i++) {
				queue.enqueue(buff.get(i));
			}

			buff.clear();
		}

		++allocatedCount;
		allocatedBytes += expectedBytes;
		return queue.dequeueInt();
	}

	public static void resizeBuffer(int delta) {
		allocatedBytes += delta;
	}

	public static void releaseBuffer(int buff, int expectedBytes) {
		GFX.deleteBuffers(buff);
		--allocatedCount;
		allocatedBytes -= expectedBytes;
	}

	public static String debugString() {
		return String.format("GL buffers: %5d %5dMb", allocatedCount, allocatedBytes / 0x100000);
	}
}
