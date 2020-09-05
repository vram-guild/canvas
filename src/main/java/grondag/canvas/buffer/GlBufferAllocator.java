package grondag.canvas.buffer;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.util.GlAllocationUtils;

/**
 * Buffer gen is incredibly slow on some Windows/NVidia systems and default MC
 * behavior
 */
public class GlBufferAllocator {
	private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue(256);
	private static final IntBuffer buff = GlAllocationUtils.allocateByteBuffer(256 * 4).asIntBuffer();
	private static int allocatedCount = 0;
	private static int allocatedBytes = 0;

	public static int claimBuffer(int expectedBytes) {
		if (queue.isEmpty()) {
			GL21.glGenBuffers(buff);

			for (int i = 0; i < 256; i++) {
				queue.enqueue(buff.get(i));
			}

			buff.clear();
		}

		++allocatedCount;
		allocatedBytes += expectedBytes;
		return queue.dequeueInt();
	}

	public static void releaseBuffer(int buff, int expectedBytes) {
		GL21.glDeleteBuffers(buff);
		--allocatedCount;
		allocatedBytes -= expectedBytes;
	}

	public static String debugString() {
		return String.format("Allocated draw buffers: %05d @ %05dMB", allocatedCount, allocatedBytes / 0x100000);
	}
}
