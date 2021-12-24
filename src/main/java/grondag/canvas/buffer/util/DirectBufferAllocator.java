/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.buffer.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public class DirectBufferAllocator {
	public static class DirectBufferReference {
		private ByteBuffer buffer;
		private final Runnable dealloc;

		private DirectBufferReference(ByteBuffer buffer, Runnable dealloc) {
			this.buffer = buffer;
			this.dealloc = dealloc;
		}

		public @Nullable ByteBuffer buffer() {
			return buffer;
		}

		public void release() {
			if (buffer != null) {
				dealloc.run();
				buffer = null;
			}
		}
	}

	private static class Deallocator implements Runnable {
		private ByteBuffer buffer;
		private final Consumer<ByteBuffer> dealloc;

		Deallocator (ByteBuffer buffer, Consumer<ByteBuffer> dealloc) {
			this.buffer = buffer;
			this.dealloc = dealloc;
		}

		@Override
		public void run() {
			synchronized (buffer) {
				if (buffer != null) {
					openBytes.addAndGet(-buffer.capacity());
					//openCount.decrementAndGet();
					dealloc.accept(buffer);
					buffer = null;
				}
			}
		}

		public void releaseIfLeaked() {
			if (buffer != null) {
				CanvasMod.LOG.warn("Memory leak detected. This should not normally occur. Bytes recovered: " + buffer.capacity());
				run();
			}
		}
	}

	//private static final AtomicReference<LinkedTransferQueue<BufferReferenceHolder>> REFERENCES = new AtomicReference<>(new LinkedTransferQueue<>());
	//private static LinkedTransferQueue<BufferReferenceHolder> idleList = new LinkedTransferQueue<>();
	private static final ReferenceQueue<DirectBufferReference> REFERENCES = new ReferenceQueue<>();
	private static final ConcurrentHashMap<PhantomReference<DirectBufferReference>, Deallocator> MAP = new ConcurrentHashMap<>();

	private static long nextCleanupTimeMilliseconds;
	//private static int lastCount;
	private static int lastBytes;
	//private static int sampleCount;
	private static int sampleBytes;
	//private static final AtomicInteger openCount = new AtomicInteger();
	private static final AtomicInteger openBytes = new AtomicInteger();
	//private static final AtomicInteger totalCount = new AtomicInteger();
	private static final AtomicInteger totalBytes = new AtomicInteger();

	public static DirectBufferReference claim(int bytes) {
		final boolean safe = Configurator.safeNativeMemoryAllocation;
		final ByteBuffer buffer = safe ? BufferUtils.createByteBuffer(bytes) : MemoryUtil.memAlloc(bytes);

		//openCount.incrementAndGet();
		openBytes.addAndGet(bytes);
		//totalCount.incrementAndGet();
		totalBytes.addAndGet(bytes);

		final Consumer<ByteBuffer> free = safe ? b -> { } : MemoryUtil::memFree;
		final var dealloc = new Deallocator(buffer, free);
		final var result = new DirectBufferReference(buffer, dealloc);
		MAP.put(new PhantomReference<>(result, REFERENCES), dealloc);
		return result;
	}

	public static void update() {
		assert RenderSystem.isOnRenderThread();

		final long time = System.currentTimeMillis();

		if (time > nextCleanupTimeMilliseconds) {
			nextCleanupTimeMilliseconds = time + 1000;

			Reference<? extends DirectBufferReference> ref;

			while ((ref = REFERENCES.poll()) != null) {
				final Deallocator dealloc = MAP.remove(ref);

				if (dealloc == null) {
					CanvasMod.LOG.error("Direct buffer reference not found for finalization");
				} else {
					dealloc.releaseIfLeaked();
				}
			}

			//final int newCount = totalCount.get();
			final int newBytes = totalBytes.get();

			//sampleCount = newCount - lastCount;
			sampleBytes = newBytes - lastBytes;
			//lastCount = newCount;
			lastBytes = newBytes;
		}
	}

	public static String debugString() {
		final String type = Configurator.safeNativeMemoryAllocation ? "Heap" : "Off-heap";

		return String.format("%s buffers:%5.1fMb rate:%5.1fMb",
				type,
				//return String.format("Off-heap buffers :%3d %5.1fMb  rate:%4d %5.1fMb",
				//openCount.get(),
				(double) openBytes.get() / 0x100000,
				//sampleCount,
				(double) sampleBytes / 0x100000
				);
	}
}
