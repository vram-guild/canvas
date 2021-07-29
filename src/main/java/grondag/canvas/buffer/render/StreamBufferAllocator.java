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

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.function.Function;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.BinIndex;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
public class StreamBufferAllocator {
	private static final IdentityHashMap<CanvasVertexFormat, BufferAllocator<StreamBuffer>> ALLOCATORS = new IdentityHashMap<>();

	static StreamBuffer claim(CanvasVertexFormat format, int bytes) {
		assert RenderSystem.isOnRenderThread();
		return ALLOCATORS.computeIfAbsent(format, binIndex -> {
			final Function<BinIndex, StreamBuffer> allocator = b -> new StreamBuffer(b, format);
			return new BufferAllocator<>("STREAM", allocator, ArrayDeque::new);
		}).claim(bytes);
	}

	public static void forceReload() {
		assert RenderSystem.isOnRenderThread();
		ALLOCATORS.values().forEach(BufferAllocator::forceReload);
	}

	static void release(StreamBuffer streamBuffer) {
		assert RenderSystem.isOnRenderThread();
		ALLOCATORS.get(streamBuffer.format).release(streamBuffer);
	}
}
