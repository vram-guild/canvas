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

public abstract class AbstractDrawableRegion<T extends DrawableDelegate> implements DrawableRegion {
	protected boolean isClosed = false;
	protected T delegate;

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

			closeInner();

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
}
