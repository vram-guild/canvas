/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
