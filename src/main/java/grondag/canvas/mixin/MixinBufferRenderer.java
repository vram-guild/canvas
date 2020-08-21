/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;

import grondag.canvas.render.RenderLayerHandler;

@Mixin(BufferRenderer.class)
public abstract class MixinBufferRenderer {
	@Redirect(method = "Lnet/minecraft/client/render/BufferRenderer;draw(Ljava/nio/ByteBuffer;ILnet/minecraft/client/render/VertexFormat;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormat;startDrawing(J)V"))
	private static void onFormatStart(VertexFormat format, long address) {
		RenderLayerHandler.onFormatStart(format, address);
	}

	@Redirect(method = "Lnet/minecraft/client/render/BufferRenderer;draw(Ljava/nio/ByteBuffer;ILnet/minecraft/client/render/VertexFormat;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormat;endDrawing()V"))
	private static void onFormatEnd(VertexFormat format) {
		RenderLayerHandler.onFormatEnd(format);
	}
}
