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

package grondag.canvas.render.region.vs;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.varia.GFX;

public class ClumpedVertexStorage {
	public static final ClumpedVertexStorage INSTANCE = new ClumpedVertexStorage(0x20000000);
	private static final int VAO_NONE = -1;

	private final ObjectArrayList<ClumpedDrawableStorage> noobs = new ObjectArrayList<>();
	private final int capacityBytes;
	private int glBufferId = -1;
	private boolean isClosed = false;

	private int head = 0;
	private int tail = 0;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = VAO_NONE;

	public ClumpedVertexStorage(int capacityBytes) {
		this.capacityBytes = capacityBytes;
	}

	public void clear() {
		assert RenderSystem.isOnRenderThread();
		head = 0;
		tail = 0;
		noobs.clear();
	}

	public void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			if (vaoBufferId > 0) {
				GFX.deleteVertexArray(vaoBufferId);
				vaoBufferId = VAO_NONE;
			}

			if (glBufferId != -1) {
				GFX.deleteBuffers(glBufferId);
				glBufferId = -1;
			}
		}
	}

	public void bind() {
		if (vaoBufferId == VAO_NONE) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			VsFormat.VS_MATERIAL.enableAttributes();
			VsFormat.VS_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	void allocate(ClumpedDrawableStorage storage) {
		assert RenderSystem.isOnRenderThread();

		noobs.add(storage);
		tail += storage.byteCount;

		if (tail >= capacityBytes) {
			assert false : "lol";
		}
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		final int len = tail - head;

		if (len > 0) {
			assert !noobs.isEmpty();

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_ARRAY_BUFFER, head, len,
					GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

			int baseOffset = 0;

			if (bBuff == null) {
				CanvasMod.LOG.warn("Unable to map buffer. If this repeats, rendering will be incorrect and is probably a compatibility issue.");
			} else {
				for (ClumpedDrawableStorage noob : noobs) {
					final ByteBuffer transferBuffer = noob.getAndClearTransferBuffer();
					final int byteCount = noob.byteCount;

					noob.setBaseVertex(head / VsFormat.VS_MATERIAL.vertexStrideBytes);
					bBuff.put(baseOffset, transferBuffer, 0, byteCount);
					TransferBufferAllocator.release(transferBuffer);
					baseOffset += byteCount;
					head += byteCount;
				}

				noobs.clear();

				assert head == tail;
				System.out.println(head + " bytes consumed");

				GFX.flushMappedBufferRange(GFX.GL_ARRAY_BUFFER, 0, len);
				GFX.unmapBuffer(GFX.GL_ARRAY_BUFFER);
			}

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		}
	}

	private int glBufferId() {
		int result = glBufferId;

		if (result == -1) {
			result = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, result);
			GFX.bufferData(GFX.GL_ARRAY_BUFFER, capacityBytes, GFX.GL_STATIC_DRAW);
			glBufferId = result;
		}

		return result;
	}
}
