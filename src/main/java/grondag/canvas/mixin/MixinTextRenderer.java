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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.mixinterface.TextRendererExt;
import grondag.canvas.render.gui.BufferBuilderStore;

@Mixin(TextRenderer.class)
public class MixinTextRenderer implements TextRendererExt {
	private Matrix4f drawMatrix4f;
	private BufferBuilder drawBuilder;
	VertexConsumerProvider.Immediate drawImmediate;

	@Override
	public void canvas_beginBatchDraw(Matrix4f fillMatrix4fIn) {
		assert drawMatrix4f == null;
		assert drawBuilder == null;
		assert drawImmediate == null;

		drawMatrix4f = fillMatrix4fIn;
		drawBuilder = BufferBuilderStore.claim();
		drawImmediate = VertexConsumerProvider.immediate(drawBuilder);
	}

	@Override
	public void canvas_endBatchDraw() {
		drawImmediate.draw();
		drawMatrix4f = null;
		drawBuilder = BufferBuilderStore.release(drawBuilder);
		drawImmediate = null;
	}

	@Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 1)
	private void onDraw(MatrixStack matrixStack, String string, float x, float y, int color, CallbackInfoReturnable<Integer> ci) {
		if (string == null) {
			ci.cancel();
		} else if (drawMatrix4f != null) {
			final TextRenderer me = (TextRenderer) (Object) this;
			me.draw(string, x, y, color, false, drawMatrix4f, drawImmediate, false, 0, 15728880, me.isRightToLeft());
			ci.cancel();
		}
	}
}
