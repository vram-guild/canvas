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

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jemalloc.JEmalloc;

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

	private static class UnsafeDeallocator implements Runnable {
		private ByteBuffer buffer;

		UnsafeDeallocator (ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void run() {
			synchronized (buffer) {
				if (buffer != null) {
					openUnsafeBytes.addAndGet(-buffer.capacity());
					openUnsafeCount.decrementAndGet();
					MemoryUtil.memFree(buffer);
					buffer = null;
				}
			}
		}
	}

	private static class UnsafeReferenceHolder {
		final WeakReference<DirectBufferReference> reference;
		final UnsafeDeallocator dealloc;

		UnsafeReferenceHolder(WeakReference<DirectBufferReference> reference, UnsafeDeallocator dealloc) {
			this.reference = reference;
			this.dealloc = dealloc;
		}

		boolean release() {
			DirectBufferReference ref = reference.get();

			if (ref == null) {
				if (dealloc.buffer != null) {
					CanvasMod.LOG.warn("Memory leak detected. This should not normally occur. Bytes recovered: " + dealloc.buffer.capacity());
					dealloc.run();
				}

				return true;
			} else {
				return ref.buffer == null;
			}
		}
	}

	private static final AtomicReference<LinkedTransferQueue<UnsafeReferenceHolder>> UNSAFE_REFS = new AtomicReference<>(new LinkedTransferQueue<>());
	private static LinkedTransferQueue<UnsafeReferenceHolder> idleList = new LinkedTransferQueue<>();
	private static long nextCleanupTimeMilliseconds;
	private static int lastCount;
	private static int lastBytes;
	private static int sampleCount;
	private static int sampleBytes;
	private static final AtomicInteger openUnsafeCount = new AtomicInteger();
	private static final AtomicInteger openUnsafeBytes = new AtomicInteger();
	private static final AtomicInteger totalUnsafeCount = new AtomicInteger();
	private static final AtomicInteger totalUnsafeBytes = new AtomicInteger();

	static {
		// Hat tip to JellySquid for this...
		// LWJGL 3.2.3 ships Jemalloc 5.2.0 which seems to be broken on Windows and suffers from critical memory leak problems
		// Using the system allocator prevents memory leaks and other problems
		// See changelog here: https://github.com/jemalloc/jemalloc/releases/tag/5.2.1
		if (Platform.get() == Platform.WINDOWS && isJEmallocPotentiallyBuggy()) {
			if (!"system".equals(Configuration.MEMORY_ALLOCATOR.get())) {
				Configuration.MEMORY_ALLOCATOR.set("system");
				CanvasMod.LOG.info("Canvas configured LWJGL to use the system memory allocator due to a potential memory leak in JEmalloc.");
			}
		}
	}

	private static boolean isJEmallocPotentiallyBuggy() {
		// done this way to make eclipse shut up in dev
		int major = JEmalloc.JEMALLOC_VERSION_MAJOR;
		int minor = JEmalloc.JEMALLOC_VERSION_MINOR;
		int patch = JEmalloc.JEMALLOC_VERSION_BUGFIX;

		if (major == 5) {
			if (minor < 2) {
				return true;
			} else if (minor == 2) {
				return patch == 0;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static DirectBufferReference claim(int bytes) {
		final boolean safe = Configurator.safeNativeMemoryAllocation;
		final ByteBuffer buffer = safe ? BufferUtils.createByteBuffer(bytes) : MemoryUtil.memAlloc(bytes);

		if (safe) {
			return new DirectBufferReference(buffer, Runnables.doNothing());
		} else {
			final var dealloc = new UnsafeDeallocator(buffer);
			var result = new DirectBufferReference(buffer, dealloc);
			var ref = new UnsafeReferenceHolder(new WeakReference<>(result), dealloc);
			UNSAFE_REFS.get().add(ref);
			openUnsafeCount.incrementAndGet();
			openUnsafeBytes.addAndGet(bytes);
			totalUnsafeCount.incrementAndGet();
			totalUnsafeBytes.addAndGet(bytes);
			return result;
		}
	}

	public static void cleanup() {
		assert RenderSystem.isOnRenderThread();

		final long time = System.currentTimeMillis();

		if (time > nextCleanupTimeMilliseconds) {
			nextCleanupTimeMilliseconds = time + 1000;

			final var newList = idleList;
			final var oldList = UNSAFE_REFS.getAndSet(idleList);
			idleList = oldList;

			if (!oldList.isEmpty()) {
				for (var ref : oldList) {
					if (!ref.release()) {
						newList.add(ref);
					}
				}

				oldList.clear();
			}

			final int newCount = totalUnsafeCount.get();
			final int newBytes = totalUnsafeBytes.get();

			sampleCount = newCount - lastCount;
			sampleBytes = newBytes - lastBytes;
			lastCount = newCount;
			lastBytes = newBytes;
		}
	}

	public static String allocationReport() {
		return String.format("Off-heap:%3d %5.1fMb  rate:%4d %5.1fMb",
				openUnsafeCount.get(),
				(double) openUnsafeBytes.get() / 0x100000,
				sampleCount,
				(double) sampleBytes / 0x100000
				);
	}
}
