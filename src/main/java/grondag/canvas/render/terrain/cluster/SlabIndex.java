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

package grondag.canvas.render.terrain.cluster;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.varia.GFX;

class SlabIndex extends AbstractGlBuffer {
	private boolean isClaimed = false;

	private SlabIndex() {
		super(Slab.BYTES_PER_SLAB_INDEX, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
	}

	void release() {
		assert RenderSystem.isOnRenderThread();
		assert isClaimed;
		isClaimed = false;
		POOL.offer(this);
	}

	@Override
	protected void onShutdown() {
		// NOOP
	}

	private static final ArrayDeque<SlabIndex> POOL = new ArrayDeque<>();
	private static SlabIndex fullSlabIndex;

	static SlabIndex claim() {
		assert RenderSystem.isOnRenderThread();

		SlabIndex result = POOL.poll();

		if (result == null) {
			result = new SlabIndex();
		} else {
			result.orphan();
		}

		result.isClaimed = true;
		return result;
	}

	static SlabIndex fullSlabIndex() {
		SlabIndex result = fullSlabIndex;

		if (result == null) {
			result = new SlabIndex();
			result.bind();
			final ByteBuffer buff = GFX.mapBuffer(result.bindTarget, GFX.GL_WRITE_ONLY);
			final ShortBuffer sink = buff.asShortBuffer();
			int i = 0;
			int v = 0;

			while (i < Slab.MAX_TRI_VERTEX_COUNT) {
				sink.put(i++, (short) v);
				sink.put(i++, (short) (v + 1));
				sink.put(i++, (short) (v + 2));
				sink.put(i++, (short) (v + 2));
				sink.put(i++, (short) (v + 3));
				sink.put(i++, (short) v);
				v += 4;
			}

			GFX.unmapBuffer(result.bindTarget);

			fullSlabIndex = result;
		}

		return result;
	}
}
