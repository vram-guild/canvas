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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

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
				Deallocator dealloc = MAP.remove(ref);

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
