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

import grondag.canvas.buffer.format.BufferVAO;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.varia.GFX;

public class StaticDrawBuffer extends AbstractGlBuffer implements UploadableVertexStorage {
	TransferBuffer transferBuffer;
	private final BufferVAO vao;

	public StaticDrawBuffer(CanvasVertexFormat format, TransferBuffer transferBuffer) {
		super(transferBuffer.sizeBytes(), GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		vao = new BufferVAO(format, () -> GFX.bindBuffer(bindTarget, glBufferId()));
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
