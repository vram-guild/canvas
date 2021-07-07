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

package grondag.canvas.vf.stream;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

public class VfStreamReference implements AutoCloseable {
	public final int byteSize;
	public final int byteAddress;
	private final VfStreamHolder holder;

	private boolean isClosed = false;

	VfStreamReference(int byteAddress, int byteSize, VfStreamHolder holder) {
		this.byteSize = byteSize;
		this.byteAddress = byteAddress;
		this.holder = holder;

		assert byteSize != 0;
		assert (byteSize & 3) == 0 : "Buffer size must be int-aligned";
	}

	public boolean isClosed() {
		return isClosed;
	}

	/** MUST BE CALLED! */
	@Override
	public void close() {
		if (!isClosed) {
			isClosed = true;
			holder.notifyClosed(this);
		}
	}

	/** Doesn't notify. */
	void markClosed() {
		isClosed = true;
	}

	public void unbind() {
		if (isClosed) {
			assert false : "Attempt to unbind closed stream reference";
		} else {
			CanvasTextureState.activeTextureUnit(holder.spec.textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	public void bind() {
		if (isClosed) {
			assert false : "Attempt to bind closed stream reference";
		} else {
			CanvasTextureState.activeTextureUnit(holder.spec.textureUnit);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, holder.textureId);
			GFX.texBuffer(holder.spec.imageFormat, holder.bufferId);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		}
	}

	public static final VfStreamReference EMPTY = new VfStreamReference(0, 4, null) {
		@Override
		public boolean isClosed() {
			return true;
		}

		@Override
		public void close() {
			// NOOP
		}

		@Override
		void markClosed() {
			// NOOP
		}

		@Override
		public void unbind() {
			// NOOP
		}

		@Override
		public void bind() {
			// NOOP
		}
	};
}
