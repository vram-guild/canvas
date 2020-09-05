package grondag.canvas.buffer;

import java.nio.IntBuffer;

import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.client.util.GlAllocationUtils;

import grondag.canvas.varia.CanvasGlHelper;

public class VaoAllocator {
	private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
	private static final IntBuffer buff = GlAllocationUtils.allocateByteBuffer(128 * 4).asIntBuffer();

	public static int claimVertexArray() {
		assert RenderSystem.isOnRenderThread();
		
		if (queue.isEmpty()) {
			CanvasGlHelper.glGenVertexArrays(buff);

			for (int i = 0; i < 128; i++) {
				queue.enqueue(buff.get(i));
			}

			buff.clear();
		}

		return queue.dequeueInt();
	}

	public static void releaseVertexArray(int vaoBufferId) {
		assert RenderSystem.isOnRenderThread();
		
		queue.enqueue(vaoBufferId);
	}
}
