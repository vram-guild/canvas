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

	protected int glBufferId() {
		int result = glBufferId;

		if (result == 0) {
			result = GlBufferAllocator.claimBuffer(capacityBytes);
			glBufferId = result;
			GFX.bindBuffer(bindTarget, result);
			GFX.bufferData(bindTarget, capacityBytes, usageHint);
			GFX.bindBuffer(bindTarget, 0);
		}

		return result;
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
