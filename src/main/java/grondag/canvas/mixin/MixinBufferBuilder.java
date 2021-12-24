/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
