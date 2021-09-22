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

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

	@Inject(method = "renderItemInHand", require = 1, at = @At("RETURN"))
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
