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

import net.minecraft.client.texture.NativeImage;

import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.mixinterface.NativeImageExt;
import grondag.canvas.varia.GFX;

@Mixin(NativeImage.class)
public class MixinNativeImage implements NativeImageExt {
	@Shadow private long sizeBytes;
	@Shadow private long pointer;

	private boolean enablePBO = false;
	private int pboBufferId = 0;

	@Override
	public void canvas_enablePBO() {
		enablePBO = true;
	}

	@Override
	public void canvas_prepareUpdatePBO() {
		if (pboBufferId == 0) {
			enablePBO = true;
			pboBufferId = GlBufferAllocator.claimBuffer((int) sizeBytes);
		}

		GFX.bindBuffer(GFX.GL_PIXEL_UNPACK_BUFFER, pboBufferId);
		GFX.unsafeBufferData(GFX.GL_PIXEL_UNPACK_BUFFER, sizeBytes, pointer, GFX.GL_STATIC_READ);
		GFX.bindBuffer(GFX.GL_PIXEL_UNPACK_BUFFER, 0);
	}

	@Inject(method = "uploadInternal", at = @At("HEAD"), cancellable = true)
	private void beforeUploadInternal(int i, int j, int k, int l, int m, int n, int o, boolean bl, boolean bl2, boolean bl3, boolean bl4, CallbackInfo ci) {
		if (enablePBO) {
			GFX.enablePBO(true);
			prepareOrBindPBO();
		}
	}

	@Inject(method = "uploadInternal", at = @At("RETURN"), cancellable = true)
	private void afterUploadInternal(int i, int j, int k, int l, int m, int n, int o, boolean bl, boolean bl2, boolean bl3, boolean bl4, CallbackInfo ci) {
		if (enablePBO) {
			unbindPBO();
			GFX.enablePBO(false);
		}
	}

	private void prepareOrBindPBO() {
		if (pboBufferId == 0) {
			pboBufferId = GlBufferAllocator.claimBuffer((int) sizeBytes);
			GFX.bindBuffer(GFX.GL_PIXEL_UNPACK_BUFFER, pboBufferId);
			GFX.unsafeBufferData(GFX.GL_PIXEL_UNPACK_BUFFER, sizeBytes, pointer, GFX.GL_STATIC_READ);
		} else {
			GFX.bindBuffer(GFX.GL_PIXEL_UNPACK_BUFFER, pboBufferId);
		}
	}

	private void unbindPBO() {
		GFX.bindBuffer(GFX.GL_PIXEL_UNPACK_BUFFER, 0);
	}

	@Inject(method = "close", at = @At("HEAD"), cancellable = false)
	private void onClose(CallbackInfo ci) {
		if (pboBufferId != 0) {
			GlBufferAllocator.releaseBuffer(pboBufferId, (int) sizeBytes);
			pboBufferId = 0;
		}
	}

	@Override
	public long canvas_pointer() {
		return pointer;
	}
}
