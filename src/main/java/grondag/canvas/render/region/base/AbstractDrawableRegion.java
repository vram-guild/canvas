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

public abstract class AbstractDrawableRegion<T extends AbstractDrawableDelegate> implements DrawableRegion {
	protected boolean isClosed = false;
	protected T delegate;
	private final AtomicInteger listRetainCount = new AtomicInteger();

	protected AbstractDrawableRegion(T delegate) {
		this.delegate = delegate;
	}

	public final T delegate() {
		return delegate;
	}

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	@Override
	public final void releaseFromRegion() {
		if (!isClosed) {
			isClosed = true;

			if (isReleasedFromDrawList()) {
				closeInner();
			}

			assert delegate != null;
			delegate.close();
			delegate = null;
		}
	}

	protected abstract void closeInner();

	@Override
	public final boolean isReleasedFromRegion() {
		return isClosed;
	}

	@Override
	public void retainFromDrawList() {
		listRetainCount.getAndIncrement();
	}

	@Override
	public void releaseFromDrawList() {
		final int count = listRetainCount.decrementAndGet();
		assert count >= 0;

		if (count == 0 && isClosed) {
			closeInner();
		}
	}

	@Override
	public boolean isReleasedFromDrawList() {
		return listRetainCount.get() == 0;
	}
}
