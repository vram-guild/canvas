/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.terrain.render;

import java.nio.IntBuffer;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DrawableChunk {
	public static DrawableChunk EMPTY_DRAWABLE = new DrawableChunk.Dummy();
	public final VboBuffer vboBuffer;
	protected boolean isClosed = false;
	protected ObjectArrayList<DrawableDelegate> delegates;

	protected DrawableChunk(VboBuffer vboBuffer, ObjectArrayList<DrawableDelegate> delegates) {
		this.vboBuffer = vboBuffer;
		this.delegates = delegates;
	}

	public ObjectArrayList<DrawableDelegate> delegates() {
		return delegates;
	}

	protected void closeInner() {
		assert delegates != null;
		clearDelegateList(delegates);
		delegates = null;
	}

	private static void clearDelegateList(ObjectArrayList<DrawableDelegate> delegates) {
		if (!delegates.isEmpty()) {
			final int limit = delegates.size();

			for (int i = 0; i < limit; i++) {
				delegates.get(i).release();
			}

			delegates.clear();
		}

		DelegateLists.releaseDelegateList(delegates);
	}

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	public final void close() {
		if (!isClosed) {
			isClosed = true;

			closeInner();

			vboBuffer.close();
		}
	}

	public final boolean isClosed() {
		return isClosed;
	}

	private static class Dummy extends DrawableChunk {
		private final ObjectArrayList<DrawableDelegate> nothing = new ObjectArrayList<>();

		protected Dummy() {
			super(null, null);
			isClosed = true;
		}

		@Override
		public ObjectArrayList<DrawableDelegate> delegates() {
			return nothing;
		}

		@Override
		protected void closeInner() {
			// NOOP
		}
	}

	public static DrawableChunk pack(VertexCollectorList collectorList, VboBuffer vboBuffer, boolean translucent) {
		// WIP: sort to appropriate draw order
		// WIP: further consolidate draw calls with same render state - use drawPriority.
		// WIP: handle conditions.
		final IntBuffer intBuffer = vboBuffer.intBuffer();
		intBuffer.position(0);

		final int limit = collectorList.size();
		int position = 0;

		final ObjectArrayList<DrawableDelegate> delegates = DelegateLists.getReadyDelegateList();

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = collectorList.get(i);

			if (collector.materialState().sorted == translucent) {
				final int vertexCount = collector.vertexCount();
				collector.toBuffer(intBuffer);
				delegates.add(DrawableDelegate.claim(collector.materialState(), position, vertexCount));
				position += vertexCount;
			}
		}

		if (delegates.isEmpty()) {
			DelegateLists.releaseDelegateList(delegates);
			return EMPTY_DRAWABLE;
		} else {
			return new DrawableChunk(vboBuffer, delegates);
		}
	}
}
