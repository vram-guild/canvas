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

import grondag.canvas.buffer.format.BufferVAO;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.varia.GFX;

public class StaticDrawBuffer extends AbstractGlBuffer implements UploadableVertexStorage {
	TransferBuffer transferBuffer;
	private final BufferVAO vao;

	public StaticDrawBuffer(CanvasVertexFormat format, TransferBuffer transferBuffer) {
		super(transferBuffer.sizeBytes(), GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		vao = new BufferVAO(format, () -> glBufferId(), () -> 0);
		this.transferBuffer = transferBuffer;
	}

	@Override
	public void upload() {
		if (transferBuffer != null) {
			GFX.bindBuffer(bindTarget, glBufferId());
			transferBuffer = transferBuffer.releaseToBoundBuffer(bindTarget, 0);
			GFX.bindBuffer(bindTarget, 0);
		}
	}

	@Override
	protected void onShutdown() {
		if (transferBuffer != null) {
			transferBuffer = transferBuffer.release();
		}

		vao.shutdown();
	}

	@Override
	public void bind() {
		vao.bind();
	}

	@Override
	public UploadableVertexStorage release() {
		shutdown();
		return null;
	}
}
