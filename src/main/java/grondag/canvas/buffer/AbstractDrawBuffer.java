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

package grondag.canvas.buffer;

import java.nio.IntBuffer;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.render.region.UploadableVertexStorage;
import grondag.canvas.varia.GFX;

abstract class AbstractDrawBuffer extends AbstractGlBuffer implements UploadableVertexStorage {
	private final BufferVAO vao;

	protected AbstractDrawBuffer(int capacityBytes, CanvasVertexFormat format, int useageHint) {
		super(capacityBytes, GFX.GL_ARRAY_BUFFER, useageHint);
		vao = new BufferVAO(format);
	}

	public void bind() {
		vao.bind(GFX.GL_ARRAY_BUFFER, glBufferId());
	}

	public abstract IntBuffer intBuffer();

	@Override
	public abstract void upload();
}
