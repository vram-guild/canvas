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
