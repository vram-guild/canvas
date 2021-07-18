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

package grondag.canvas.mixin;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

import grondag.canvas.mixinterface.BufferBuilderExt;

/**
 * BufferRenderer tends to assume nothing else has touched bindings
 * and implements bind state caching.
 * We change state elsewhere so we save and restore this as needed.
 */
@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderExt {
	@Shadow private VertexFormat format;
	@Shadow private ByteBuffer buffer;
	@Shadow private int vertexCount;
	@Shadow private int currentElementId;
	@Shadow private boolean building;
	@Shadow private int elementOffset;

	@Shadow private void grow(int i) { }

	private ByteBuffer lastByteBuffer;
	private IntBuffer intBuffer;

	@Override
	public boolean canvas_canSupportDirect(VertexFormat expectedFormat) {
		return building && format == expectedFormat && currentElementId == 0;
	}

	private IntBuffer getIntBuffer() {
		if (buffer != lastByteBuffer) {
			intBuffer = buffer.asIntBuffer();
		}

		return intBuffer;
	}

	@Override
	public void canvas_putQuadDirect(int[] data) {
		assert currentElementId == 0;
		grow(format.getVertexSize() * 4);
		vertexCount += 4;
		getIntBuffer().put(elementOffset / 4, data);
		elementOffset += data.length * 4;
	}
}
