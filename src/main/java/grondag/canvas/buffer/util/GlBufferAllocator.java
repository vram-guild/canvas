/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.util;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import com.mojang.blaze3d.platform.MemoryTracker;

import grondag.canvas.varia.GFX;

public class GlBufferAllocator {
	private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue(256);
	private static final IntBuffer buff = MemoryTracker.create(256 * 4).asIntBuffer();
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
