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

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.varia.GFX;

/**
 * BufferRenderer tends to assume nothing else has touched bindings
 * and implements bind state caching.
 * We change state elsewhere so we save and restore this as needed.
 */
@Mixin(BufferUploader.class)
public class MixinBufferUploader {
	@Shadow private static int lastVertexArrayObject;
	@Shadow private static int lastVertexBufferObject;
	@Shadow private static int lastIndexBufferObject;
	@Shadow private static VertexFormat lastFormat;

	private static void retoreBindings() {
		if (lastFormat != null) {
			GFX.bindVertexArray(lastVertexArrayObject);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, lastVertexBufferObject);
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, lastIndexBufferObject);
		}
	}

	@Inject(at = @At("HEAD"), method = "reset")
	private static void onReset(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "invalidateElementArrayBufferBinding")
	private static void onInvalidateElementArrayBufferBinding(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "_end")
	private static void onEnd(CallbackInfo ci) {
		retoreBindings();
	}

	@Inject(at = @At("HEAD"), method = "_endInternal")
	private static void onEndInternal(CallbackInfo ci) {
		retoreBindings();
	}
}
