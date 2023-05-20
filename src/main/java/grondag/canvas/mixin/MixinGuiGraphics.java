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

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.LevelRendererExt;
import grondag.canvas.mixinterface.RenderBuffersExt;
import grondag.canvas.render.world.CanvasWorldRenderer;

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics {
	@Shadow @Mutable @Final private MultiBufferSource.BufferSource bufferSource;

	private MultiBufferSource.BufferSource itemImmediate;

	@Inject(method = "Lnet/minecraft/client/gui/GuiGraphics;<init>(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", at = @At("TAIL"))
	void afterNew(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource _bufferSource, CallbackInfo ci) {
		final var cwr = CanvasWorldRenderer.instance();

		if (cwr != null && _bufferSource instanceof CanvasImmediate immediate) {
			final var cwrExt = (LevelRendererExt) cwr;
			bufferSource = ((RenderBuffersExt) cwrExt.canvas_bufferBuilders()).canvas_getBufferSource();
			itemImmediate = immediate;
		} else {
			bufferSource = itemImmediate = _bufferSource;
		}
	}

	@Redirect(method = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
				at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
	MultiBufferSource.BufferSource getItemImmediate(GuiGraphics instance) {
		return itemImmediate;
	}

	@Inject(method = "flush()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"))
	void flushItemImmediate(CallbackInfo ci) {
		if (itemImmediate != bufferSource) {
			itemImmediate.endBatch();
		}
	}
}
