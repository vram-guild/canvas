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

package grondag.canvas.render.terrain.base;

import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.UploadableVertexStorage;

public abstract class AbstractDrawableRegion<T extends UploadableVertexStorage> implements DrawableRegion, UploadableRegion {
	// first reference is for the region
	private final AtomicInteger retainCount = new AtomicInteger(1);
	protected T storage;
	private final int quadVertexCount;
	private boolean isClosed = false;

	protected AbstractDrawableRegion(int quadVertexCount, T storage) {
		this.quadVertexCount = quadVertexCount;
		this.storage = storage;
	}

	@Override
	public final int quadVertexCount() {
		return quadVertexCount;
	}

	@Override
	public final T storage() {
		return storage;
	}

	private void close() {
		final int retainCount = this.retainCount.decrementAndGet();
		assert retainCount >= 0;

		if (retainCount == 0) {
			assert RenderSystem.isOnRenderThread();
			isClosed = true;
			storage.release();
			storage = null;

			closeInner();
		}
	}

	public final boolean isClosed() {
		return isClosed;
	}

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	@Override
	public final void releaseFromRegion() {
		close();
	}

	protected abstract void closeInner();

	@Override
	public void retainFromDrawList() {
		final int count = retainCount.getAndIncrement();
		assert count >= 1 : "Draw list retained closed region";
	}

	@Override
	public void releaseFromDrawList() {
		close();
	}
}
