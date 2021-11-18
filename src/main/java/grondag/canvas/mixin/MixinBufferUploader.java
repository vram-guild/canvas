/*
 * Copyright © Original Authors
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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

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
