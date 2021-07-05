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

package grondag.canvas.render.region;

import java.nio.IntBuffer;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.state.RenderState;

public class DrawableRegion {
	public static DrawableRegion EMPTY_DRAWABLE = new DrawableRegion.Dummy();
	public final VboBuffer vboBuffer;
	protected boolean isClosed = false;
	protected ObjectArrayList<DrawableDelegate> delegates;

	protected DrawableRegion(VboBuffer vboBuffer, ObjectArrayList<DrawableDelegate> delegates) {
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

	private static class Dummy extends DrawableRegion {
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

	private static final Predicate<RenderState> TRANSLUCENT = m -> m.target == MaterialTarget.TRANSLUCENT && m.primaryTargetTransparency;
	private static final Predicate<RenderState> SOLID = m -> !TRANSLUCENT.test(m);

	public static DrawableRegion pack(VertexCollectorList collectorList, VboBuffer vboBuffer, boolean translucent) {
		final IntBuffer intBuffer = vboBuffer.intBuffer();
		intBuffer.position(0);
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TRANSLUCENT : SOLID);
		final int limit = drawList.size();
		int position = 0;
		final ObjectArrayList<DrawableDelegate> delegates = DelegateLists.getReadyDelegateList();

		for (int i = 0; i < limit; ++i) {
			final ArrayVertexCollector collector = drawList.get(i);

			if (collector.renderState.sorted == translucent) {
				final int vertexCount = collector.quadCount() * 4;
				collector.toBuffer(intBuffer);
				delegates.add(DrawableDelegate.claim(collector.renderState, position, vertexCount));
				position += vertexCount;
			}
		}

		if (delegates.isEmpty()) {
			DelegateLists.releaseDelegateList(delegates);
			return EMPTY_DRAWABLE;
		} else {
			return new DrawableRegion(vboBuffer, delegates);
		}
	}
}
