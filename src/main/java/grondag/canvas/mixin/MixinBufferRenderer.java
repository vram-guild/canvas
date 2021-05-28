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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;

import grondag.canvas.varia.GFX;

/**
 * BufferRenderer tends to assume nothing else has touched bindings
 * and implements bind state caching.
 * We change state elsewhere so we save and restore this as needed.
 */
@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
	@Shadow private static int currentVertexArray;
	@Shadow private static int currentVertexBuffer;
	@Shadow private static int currentElementBuffer;
	@Shadow private static VertexFormat vertexFormat;

	private static void retoreBindings() {
		if (vertexFormat != null) {
			GFX.bindVertexArray(currentVertexArray);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, currentVertexBuffer);
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, currentElementBuffer);
		}
	}

	@Inject(at = @At("HEAD"), method = "unbindAll")
	private static void onUnbindAll(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "unbindElementBuffer")
	private static void onUnbindElementBuffer(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "draw")
	private static void onDraw(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "postDraw")
	private static void onPostDraw(CallbackInfo ci) {
		retoreBindings();
	}
}
