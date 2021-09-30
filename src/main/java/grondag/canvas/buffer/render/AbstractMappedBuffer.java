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

package grondag.canvas.buffer.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

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
	private ShortBuffer mappedShortBuffer;
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

	public final ByteBuffer byteBuffer() {
		return mappedBuffer;
	}

	public final ShortBuffer shortBuffer() {
		assert claimedBytes > 0 : "Buffer accessed while unclaimed";
		assert mappedBuffer != null;
		ShortBuffer result = mappedShortBuffer;

		if (result == null) {
			result = mappedBuffer.asShortBuffer();
			mappedShortBuffer = result;
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
				mappedShortBuffer = null;
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
			mappedBuffer = null;
			mappedIntBuffer = null;
			mappedShortBuffer = null;
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
