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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.BufferDebug;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.shader.data.ScreenRenderState;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements GameRendererExt {
	@Shadow float zoom;
	@Shadow float zoomX;
	@Shadow float zoomY;
	@Shadow int tick;
	@Shadow protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);
	@Shadow protected abstract void bobHurt(PoseStack matrixStack, float f);
	@Shadow protected abstract void bobView(PoseStack matrixStack, float f);
	@Shadow private Minecraft minecraft;

	@Inject(method = "renderLevel", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V", shift = At.Shift.AFTER))
	private void afterRenderHand(CallbackInfo ci) {
		ScreenRenderState.setRenderingHand(false);
		PipelineManager.afterRenderHand();

		if (Configurator.enableBufferDebug) {
			BufferDebug.render();
		}
	}

	@Inject(method = "getFov", require = 1, at = @At("RETURN"))
	private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> ci) {
		((CanvasWorldRenderer) minecraft.levelRenderer).updateProjection(camera, tickDelta, ci.getReturnValueD());
	}

	@Inject(method = "renderLevel", require = 1, at = @At("HEAD"))
	private void onRenderLevel(CallbackInfo ci) {
		Timekeeper.instance.startFrame(Timekeeper.ProfilerGroup.GameRendererSetup, "GameRenderer_setup");
	}

	@Override
	public float canvas_zoom() {
		return zoom;
	}

	@Override
	public float canvas_zoomX() {
		return zoomX;
	}

	@Override
	public float canvas_zoomY() {
		return zoomY;
	}

	@Override
	public double canvas_getFov(Camera camera, float tickDelta, boolean changingFov) {
		return getFov(camera, tickDelta, changingFov);
	}

	@Override
	public void canvas_bobViewWhenHurt(PoseStack matrixStack, float f) {
		bobHurt(matrixStack, f);
	}

	@Override
	public void canvas_bobView(PoseStack matrixStack, float f) {
		bobView(matrixStack, f);
	}

	@Override
	public int canvas_ticks() {
		return tick;
	}

	//	@Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
	//	private void preventClearOnDebug(int mask, boolean getError) {
	//		//RenderSystem.clear(mask, getError);
	//	}
}
