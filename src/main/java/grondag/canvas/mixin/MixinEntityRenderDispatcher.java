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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.EntityRenderDispatcherExt;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher implements EntityRenderDispatcherExt {
	@Shadow private static RenderType SHADOW_RENDER_TYPE;

	@Override
	public RenderType canvas_shadowLayer() {
		return SHADOW_RENDER_TYPE;
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void beginRender(Entity entity, double d, double e, double f, float g, float h, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
		if (multiBufferSource instanceof CanvasImmediate immediate) {
			immediate.contextState.push(entity);
		}
	}

	@Inject(at = @At("RETURN"), method = "render")
	private void endRender(Entity entity, double d, double e, double f, float g, float h, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
		if (multiBufferSource instanceof CanvasImmediate immediate) {
			immediate.contextState.pop();
		}
	}
}
