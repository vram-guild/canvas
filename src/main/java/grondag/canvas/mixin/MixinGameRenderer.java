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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.GameMode;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.GameRendererExt;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.BufferDebug;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.render.CanvasWorldRenderer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements GameRendererExt {
	@Shadow float zoom;
	@Shadow float zoomX;
	@Shadow float zoomY;
	@Shadow int ticks;
	@Shadow protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);
	@Shadow protected abstract void bobViewWhenHurt(MatrixStack matrixStack, float f);
	@Shadow protected abstract void bobView(MatrixStack matrixStack, float f);
	@Shadow private MinecraftClient client;

	//WIP2: remove below
	@Shadow private boolean renderingPanorama;
	@Shadow private LightmapTextureManager lightmapTextureManager;
	@Shadow private BufferBuilderStorage buffers;

	// WIP2: put back
	//	@Inject(method = "renderHand", require = 1, at = @At("RETURN"))
	//	private void afterRenderHand(CallbackInfo ci) {
	//		PipelineManager.afterRenderHand();
	//
	//		if (Configurator.enableBufferDebug) {
	//			BufferDebug.render();
	//		}
	//	}

	// WIP2: remove
	@Overwrite
	private void renderHand(MatrixStack matrices, Camera camera, float tickDelta) {
		@SuppressWarnings("resource")
		final GameRenderer me = (GameRenderer) (Object) this;

		if (!renderingPanorama) {
			me.loadProjectionMatrix(me.getBasicProjectionMatrix(getFov(camera, tickDelta, false)));
			final MatrixStack.Entry entry = matrices.peek();
			entry.getModel().loadIdentity();
			entry.getNormal().loadIdentity();
			matrices.push();
			bobViewWhenHurt(matrices, tickDelta);

			if (client.options.bobView) {
				bobView(matrices, tickDelta);
			}

			final boolean bl = client.getCameraEntity() instanceof LivingEntity && ((LivingEntity) client.getCameraEntity()).isSleeping();

			if (client.options.getPerspective().isFirstPerson() && !bl && !client.options.hudHidden && client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
				lightmapTextureManager.enable();
				//me.firstPersonRenderer.renderItem(tickDelta, matrices, buffers.getEntityVertexConsumers(), client.player, client.getEntityRenderDispatcher().getLight(client.player, tickDelta));
				lightmapTextureManager.disable();
			}

			matrices.pop();

			if (client.options.getPerspective().isFirstPerson() && !bl) {
				//InGameOverlayRenderer.renderOverlays(client, matrices);
				bobViewWhenHurt(matrices, tickDelta);
			}

			if (client.options.bobView) {
				bobView(matrices, tickDelta);
			}
		}

		PipelineManager.afterRenderHand();

		if (Configurator.enableBufferDebug) {
			BufferDebug.render();
		}
	}

	@Inject(method = "getFov", require = 1, at = @At("RETURN"))
	private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> ci) {
		((CanvasWorldRenderer) client.worldRenderer).terrainFrustum.updateProjection(camera, tickDelta);
		((CanvasWorldRenderer) client.worldRenderer).terrainFrustum.setFov(ci.getReturnValueD());
	}

	@Inject(method = "renderWorld", require = 1, at = @At("HEAD"))
	private void onRenderWorld(CallbackInfo ci) {
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
	public void canvas_bobViewWhenHurt(MatrixStack matrixStack, float f) {
		bobViewWhenHurt(matrixStack, f);
	}

	@Override
	public void canvas_bobView(MatrixStack matrixStack, float f) {
		bobView(matrixStack, f);
	}

	@Override
	public int canvas_ticks() {
		return ticks;
	}

	//	@Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
	//	private void preventClearOnDebug(int mask, boolean getError) {
	//		//RenderSystem.clear(mask, getError);
	//	}
}
