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

package grondag.canvas.vf.stream;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import grondag.canvas.CanvasMod;
import grondag.canvas.varia.GFX;
import grondag.canvas.vf.BufferWriter;

class VfStreamHolder {
	final VfStreamSpec spec;

	private final ReferenceOpenHashSet<VfStreamReference> references = new ReferenceOpenHashSet<>();

	private IntBuffer iBuff;
	private int headByteOffset = 0;
	int bufferId;
	int textureId;
	int intIndex;

	/** References might all be closed while we still have capacity, so don't close until set. */
	private boolean isDetached = false;

	VfStreamHolder(VfStreamSpec spec) {
		this.spec = spec;
	}

	void detach() {
		assert !isDetached;
		isDetached = true;
	}

	void close() {
		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}

		if (textureId != 0) {
			GFX.deleteTexture(textureId);
			textureId = 0;
		}

		if (!references.isEmpty()) {
			for (VfStreamReference ref : references) {
				ref.markClosed();
			}

			references.clear();
		}

		isDetached = true;
	}

	int capacity() {
		return spec.imageCapacityBytes() - headByteOffset;
	}

	void prepare() {
		assert iBuff == null;

		if (bufferId == 0) {
			bufferId = GFX.genBuffer();
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
			GFX.bufferData(GFX.GL_TEXTURE_BUFFER, spec.imageCapacityBytes() - headByteOffset, GFX.GL_STATIC_DRAW);

			assert textureId == 0;
			textureId = GFX.genTexture();
		} else {
			GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, bufferId);
		}

		final ByteBuffer bBuff = GFX.mapBufferRange(GFX.GL_TEXTURE_BUFFER, headByteOffset, capacity(),
				GFX.GL_MAP_WRITE_BIT | GFX.GL_MAP_UNSYNCHRONIZED_BIT | GFX.GL_MAP_FLUSH_EXPLICIT_BIT | GFX.GL_MAP_INVALIDATE_RANGE_BIT);

		if (bBuff == null) {
			CanvasMod.LOG.warn("Unable to map buffer. If this repeats, rendering will be incorrect and is probably a compatibility issue.");
			iBuff = null;
		} else {
			iBuff = bBuff.asIntBuffer();
		}

		intIndex = 0;
	}

	void flush() {
		assert iBuff != null;

		if (intIndex > 0) {
			GFX.flushMappedBufferRange(GFX.GL_TEXTURE_BUFFER, 0, intIndex * 4);
			intIndex = 0;
		}

		GFX.unmapBuffer(GFX.GL_TEXTURE_BUFFER);
		GFX.bindBuffer(GFX.GL_TEXTURE_BUFFER, 0);
		iBuff = null;
	}

	/** Returns VfStreamReference.EMPTY if insufficient capacity. */
	VfStreamReference allocate(int byteCount, BufferWriter writer) {
		assert (byteCount & 3) == 0 : "Buffer allocations must be int-aligned";
		final int newHead = headByteOffset + byteCount;

		if (newHead > spec.imageCapacityBytes()) {
			return VfStreamReference.EMPTY;
		}

		VfStreamReference result = new VfStreamReference(headByteOffset, byteCount, this);

		assert iBuff != null;

		writer.write(iBuff, intIndex);

		references.add(result);
		intIndex += byteCount / 4;
		headByteOffset = newHead;

		return result;
	}

	void notifyClosed(VfStreamReference vfStreamReference) {
		if (!references.remove(vfStreamReference)) {
			assert false : "Reference not found in holder on close";
		}

		if (references.isEmpty() && isDetached) {
			close();
		}
	}
}
