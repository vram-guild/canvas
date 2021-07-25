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

package grondag.canvas.buffer.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.buffer.util.BufferSynchronizer;
import grondag.canvas.buffer.util.BufferSynchronizer.SynchronizedBuffer;
import grondag.canvas.buffer.util.BufferTrace;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.GFX;

public class AbstractMappedBuffer<T extends AbstractMappedBuffer<T>> extends AbstractGlBuffer implements AllocatableBuffer, SynchronizedBuffer {
	final BinIndex binIndex;
	private ByteBuffer mappedBuffer;
	private IntBuffer mappedIntBuffer;
	private int claimedBytes;
	private final Consumer<T> releaseQueue;
	private final BufferTrace trace = BufferTrace.create();
	private final boolean immutable;

	/** Signal that this buffer has been pre-mapped so that it can be populated off-thread. */
	private volatile boolean isPreMapped = false;

	protected AbstractMappedBuffer(BinIndex binIndex, int bindTarget, int usageHint, Consumer<T> releaseQueue) {
		super(binIndex.capacityBytes(), bindTarget, usageHint);
		this.binIndex = binIndex;
		this.releaseQueue = releaseQueue;
		immutable = CanvasGlHelper.supportsPersistentMapped();
		
		if (immutable) {
			// Force buffer creation early
			glBufferId();
			assert mappedBuffer != null;
		}
	}
	
	@Override
	protected void createBuffer() {
		if (immutable) {
			GFX.bufferStorage(bindTarget, capacityBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_PERSISTENT_BIT);
			mappedBuffer = GFX.mapBufferRange(bindTarget, 0, capacityBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_PERSISTENT_BIT);
		} else {
			super.createBuffer();
		}
	}

	@Override
	public final void prepare(int claimedBytes) {
		// NB <= is because of pre-mapped buffers
		assert this.claimedBytes == 0 || isPreMapped : "Buffer claimed more than once";
		assert claimedBytes > 0 : "Buffer claimed with zero bytes";

		if (!isPreMapped && !immutable) {
			assert RenderSystem.isOnRenderThread();

			// Map buffer.
			// NB: On Windows, the GL_MAP_INVALIDATE_BUFFER_BIT does not seem to orphan the buffer reliably
			// when GL_MAP_UNSYNCHRONIZED_BIT is also present, so we now use explicit sync instead of orphaning.
			// Frequent orphaning may cause some drivers to put important buffers in client memory.

			// NB: We don't call bind() here because it can be overridden to use a VAO for drawable buffers
			// and that can cause an invalid operation because we it doesn't actually leave the buffer bound.
			GFX.bindBuffer(bindTarget, glBufferId());
			assert mappedBuffer == null;
			mappedBuffer = GFX.mapBufferRange(bindTarget, 0, claimedBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
			GFX.bindBuffer(bindTarget, 0);
		}

		assert mappedBuffer != null;
		this.claimedBytes = claimedBytes;
	}

	public void prepareForOffThreadUse() {
		assert RenderSystem.isOnRenderThread();

		if (!immutable) {
			assert !isPreMapped : "Pre-mapped buffer improperly unmapped";
			isPreMapped = true;
			GFX.bindBuffer(bindTarget, glBufferId());
			assert mappedBuffer == null;
			mappedBuffer = GFX.mapBufferRange(bindTarget, 0, capacityBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
			assert mappedBuffer != null;
			GFX.bindBuffer(bindTarget, 0);
		}
	}

	public final int sizeBytes() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		return claimedBytes;
	}

	public final IntBuffer intBuffer() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		assert mappedBuffer != null;
		IntBuffer result = mappedIntBuffer;

		if (result == null) {
			result = mappedBuffer.asIntBuffer();
			mappedIntBuffer = result;
		}

		return result;
	}

	/**
	 * Un-map and leaves buffer bound if mapped.
	 * Return true if buffer is bound.
	 * Return false if buffer is not bound.
	 */
	protected final boolean unmap() {
		if (mappedBuffer == null) {
			return false;
		} else {
			isPreMapped = false;
			GFX.bindBuffer(bindTarget, glBufferId());
			GFX.flushMappedBufferRange(bindTarget, 0, claimedBytes);
			
			if (!immutable) {
				GFX.unmapBuffer(bindTarget);
				mappedBuffer = null;
				mappedIntBuffer = null;
			}
			
			return true;
		}
	}

	public final @Nullable T release() {
		assert claimedBytes > 0 : "Buffer released while unclaimed";
		
		if (unmap()) {
			GFX.bindBuffer(bindTarget, 0);
		}
		
		claimedBytes = 0;
		BufferSynchronizer.accept(this);
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onBufferSync() {
		releaseQueue.accept((T) this);
	}

	@Override
	protected void onShutdown() {
		if (immutable) {
			GFX.bindBuffer(bindTarget, glBufferId());
			GFX.unmapBuffer(bindTarget);
			GFX.bindBuffer(bindTarget, 0);
		} else {
			unmap();
		}
	}

	@Override
	public final BinIndex binIndex() {
		return binIndex;
	}

	@Override
	public BufferTrace trace() {
		return trace;
	}
}
