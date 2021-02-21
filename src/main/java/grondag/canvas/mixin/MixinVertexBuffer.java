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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.varia.MatrixState;

@Mixin(VertexBuffer.class)
public class MixinVertexBuffer {
	// Unmanaged draws during world rendering expect the view matrix to include
	// camera rotation but we apply that to the GL state directly - it's not part of the matrix.
	@Inject(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;multMatrix(Lnet/minecraft/util/math/Matrix4f;)V"), cancellable = false)
	private void onDraw(Matrix4f matrix, int mode, CallbackInfo ci) {
		MatrixState.applyViewIfNeeded();
	}
}
