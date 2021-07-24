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
import grondag.canvas.buffer.util.BufferTrace;
import grondag.canvas.varia.GFX;

public class AbstractMappedBuffer<T extends AbstractMappedBuffer<T>> extends AbstractGlBuffer implements AllocatableBuffer {
	final BinIndex binIndex;
	private ByteBuffer mappedBuffer;
	private IntBuffer mappedIntBuffer;
	private int claimedBytes;
	private final Consumer<T> releaseQueue;
	private final BufferTrace trace = BufferTrace.create();

	/** Signal that this buffer has been pre-mapped so that it can be populated off-thread. */
	private volatile boolean isPreMapped = false;

	protected AbstractMappedBuffer(BinIndex binIndex, int bindTarget, int usageHint, Consumer<T> releaseQueue) {
		super(binIndex.capacityBytes(), bindTarget, usageHint);
		this.binIndex = binIndex;
		this.releaseQueue = releaseQueue;
	}

	@Override
	public final void prepare(int claimedBytes) {
		// NB <= is because of pre-mapped buffers
		assert this.claimedBytes == 0 || isPreMapped : "Buffer claimed more than once";
		assert claimedBytes > 0 : "Buffer claimed with zero bytes";

		if (!isPreMapped) {
			assert RenderSystem.isOnRenderThread();

			// Invalidate and map buffer.
			// On Windows, the GL_MAP_INVALIDATE_BUFFER_BIT does not seem to orphan the buffer reliably
			// when GL_MAP_UNSYNCHRONIZED_BIT is also present, so we orphan the buffer the old-fashioned way.
			bindAndOrphan();
			mappedBuffer = GFX.mapBufferRange(bindTarget, 0, claimedBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
			GFX.bindBuffer(bindTarget, 0);
		}

		this.claimedBytes = claimedBytes;
	}

	public void prepareForOffThreadUse() {
		assert !isPreMapped : "Pre-mapped buffer improperly unmapped";
		assert RenderSystem.isOnRenderThread();

		// Invalidate and map buffer.
		// On Windows, the GL_MAP_INVALIDATE_BUFFER_BIT does not seem to orphan the buffer reliably
		// when GL_MAP_UNSYNCHRONIZED_BIT is also present, so we orphan the buffer the old-fashioned way.
		// In this path we map the entire buffer because we don't know in advance how much will be used.
		isPreMapped = true;
		bindAndOrphan();
		mappedBuffer = GFX.mapBufferRange(bindTarget, 0, capacityBytes, GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT);
		GFX.bindBuffer(bindTarget, 0);
	}

	public final int sizeBytes() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		return claimedBytes;
	}

	public final IntBuffer intBuffer() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		IntBuffer result = mappedIntBuffer;

		if (result == null) {
			result = mappedBuffer.asIntBuffer();
			mappedIntBuffer = result;
		}

		return result;
	}

	/**
	 * Un-map and leaves buffer bound if mapped.
	 * Return true if was mapped and buffer is bound.
	 * Return false if nothing happened.
	 */
	protected final boolean unmap() {
		if (mappedBuffer == null) {
			return false;
		} else {
			isPreMapped = false;
			GFX.bindBuffer(bindTarget, glBufferId());
			GFX.flushMappedBufferRange(bindTarget, 0, claimedBytes);
			GFX.unmapBuffer(bindTarget);
			mappedBuffer = null;
			mappedIntBuffer = null;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public final @Nullable T release() {
		assert claimedBytes > 0 : "Buffer released while unclaimed";
		unmap();
		GFX.bindBuffer(bindTarget, 0);
		claimedBytes = 0;
		releaseQueue.accept((T) this);
		return null;
	}

	@Override
	protected void onShutdown() {
		unmap();
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
