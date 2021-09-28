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
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;

import net.minecraft.util.Mth;

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
	@Shadow private int vertices;
	@Shadow private int elementIndex;
	@Shadow private boolean building;
	@Shadow private int nextElementByte;
	@Shadow private List<BufferBuilder.DrawState> drawStates;
	@Shadow private int lastPoppedStateIndex;
	@Shadow private int totalUploadedBytes;

	@Shadow private void ensureCapacity(int i) { }

	private ByteBuffer lastByteBuffer;
	private IntBuffer intBuffer;
	private boolean repeatableDraw = false;

	@Override
	public boolean canvas_canSupportDirect(VertexFormat expectedFormat) {
		return building && format == expectedFormat && elementIndex == 0;
	}

	private IntBuffer getIntBuffer() {
		if (buffer != lastByteBuffer) {
			intBuffer = buffer.asIntBuffer();
		}

		return intBuffer;
	}

	@Override
	public void canvas_putQuadDirect(int[] data) {
		assert elementIndex == 0;
		ensureCapacity(format.getVertexSize() * 4);
		vertices += 4;
		getIntBuffer().put(nextElementByte / 4, data);
		nextElementByte += data.length * 4;
	}

	@Override
	public void canvas_enableRepeatableDraw(boolean enable) {
		repeatableDraw = enable;
	}

	@Inject(method = "popNextBuffer", require = 1, cancellable = true, at = @At("HEAD"))
	private void onPopNextBuffer(CallbackInfoReturnable<Pair<BufferBuilder.DrawState, ByteBuffer>> ci) {
		if (repeatableDraw) {
			final BufferBuilder.DrawState drawArrayParameters = drawStates.get(lastPoppedStateIndex++);
			buffer.position(totalUploadedBytes);
			totalUploadedBytes += Mth.roundToward(drawArrayParameters.bufferSize(), 4);
			buffer.limit(totalUploadedBytes);

			if (lastPoppedStateIndex == drawStates.size() && vertices == 0) {
				totalUploadedBytes = 0;
				lastPoppedStateIndex = 0;
			}

			final ByteBuffer byteBuffer = buffer.slice();
			buffer.clear();
			ci.setReturnValue(Pair.of(drawArrayParameters, byteBuffer));
		}
	}
}
