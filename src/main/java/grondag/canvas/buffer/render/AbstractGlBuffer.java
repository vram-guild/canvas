/*
 * Copyright © Contributing Authors
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

package grondag.canvas.buffer.render;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.varia.GFX;

public abstract class AbstractGlBuffer {
	private int glBufferId = 0;
	protected final int capacityBytes;
	protected boolean isClosed = false;
	protected final int bindTarget;
	protected final int usageHint;

	protected AbstractGlBuffer(int capacityBytes, int bindTarget, int usageHint) {
		this.capacityBytes = capacityBytes;
		this.bindTarget = bindTarget;
		this.usageHint = usageHint;
	}

	public int capacityBytes() {
		return capacityBytes;
	}

	public void bind() {
		GFX.bindBuffer(bindTarget, glBufferId());
	}

	public void unbind() {
		GFX.bindBuffer(bindTarget, 0);
	}

	public final int glBufferId() {
		int result = glBufferId;

		if (result == 0) {
			result = GlBufferAllocator.claimBuffer(capacityBytes);
			glBufferId = result;
			GFX.bindBuffer(bindTarget, result);
			createBuffer();
			GFX.bindBuffer(bindTarget, 0);
		}

		return result;
	}

	protected void createBuffer() {
		GFX.bufferData(bindTarget, capacityBytes, usageHint);
	}

	//	/** Leaves buffer bound. */
	//	public void bindAndOrphan() {
	//		if (glBufferId == 0) {
	//			glBufferId = GlBufferAllocator.claimBuffer(capacityBytes);
	//		}
	//
	//		GFX.bindBuffer(bindTarget, glBufferId);
	//		GFX.bufferData(bindTarget, capacityBytes, usageHint);
	//	}

	public final void shutdown() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			onShutdown();

			if (glBufferId != 0) {
				GlBufferAllocator.releaseBuffer(glBufferId, capacityBytes);
				glBufferId = 0;
			}
		}
	}

	protected abstract void onShutdown();
}
