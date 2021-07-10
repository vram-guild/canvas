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

package grondag.canvas.render.region.base;

import java.util.concurrent.atomic.AtomicInteger;

import grondag.canvas.render.region.DrawableRegion;

public abstract class AbstractDrawableRegion<T extends AbstractDrawableState> implements DrawableRegion {
	protected T drawState;
	// first reference is for the region
	private final AtomicInteger retainCount = new AtomicInteger(1);
	private final long packedOriginBlockPos;

	protected AbstractDrawableRegion(T delegate, long packedOriginBlockPos) {
		this.drawState = delegate;
		this.packedOriginBlockPos = packedOriginBlockPos;
	}

	public final T drawState() {
		return drawState;
	}

	public long packedOriginBlockPos() {
		return packedOriginBlockPos;
	}

	private void release() {
		final int retainCount = this.retainCount.decrementAndGet();
		assert retainCount >= 0;

		if (retainCount == 0) {
			closeInner();

			assert drawState != null;
			drawState.close();
			drawState = null;
		}
	}

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	@Override
	public final void releaseFromRegion() {
		release();
	}

	protected abstract void closeInner();

	@Override
	public void retainFromDrawList() {
		final int count = retainCount.getAndIncrement();
		assert count >= 1 : "Draw list retained closed region";
	}

	@Override
	public void releaseFromDrawList() {
		release();
	}
}
