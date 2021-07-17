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

import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.BinIndex;
import grondag.canvas.varia.GFX;

public class MappedTransferBuffer extends AbstractMappedBuffer implements NewTransferBuffer {
	public MappedTransferBuffer(BinIndex binIndex) {
		super(binIndex, GFX.GL_COPY_READ_BUFFER, GFX.GL_STREAM_READ);
	}

	@Override
	public @Nullable MappedTransferBuffer release() {
		unmap();
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
		RENDER_THREAD_ALLOCATOR.release(this);
		return null;
	}

	@Override
	public void put(int[] source, int sourceStartInts, int targetStartInts, int lengthInts) {
		intBuffer().put(targetStartInts, source, sourceStartInts, lengthInts);
	}

	@Override
	public @Nullable NewTransferBuffer releaseToBoundBuffer(int target, int targetStartBytes) {
		unmap();
		GFX.copyBufferSubData(GFX.GL_COPY_READ_BUFFER, target, 0, targetStartBytes, sizeBytes());
		GFX.bindBuffer(GFX.GL_COPY_READ_BUFFER, 0);
		RENDER_THREAD_ALLOCATOR.release(this);
		return null;
	}

	public static final RenderThreadBufferAllocator<MappedTransferBuffer> RENDER_THREAD_ALLOCATOR = new RenderThreadBufferAllocator<>(MappedTransferBuffer::new);
}
