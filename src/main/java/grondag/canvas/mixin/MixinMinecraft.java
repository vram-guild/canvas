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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.MainTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.canvas.render.PrimaryFrameBuffer;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.varia.CanvasGlHelper;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable> implements MinecraftClientExt {
	@Shadow ItemColors itemColors;

	protected MixinMinecraft(String dummy) {
		super(dummy);
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void hookInit(CallbackInfo info) {
		CanvasGlHelper.init();
	}

	@Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"), method = "runTick", require = 1, allow = 1)
	private void onYield() {
		if (!Configurator.greedyRenderThread) {
			Thread.yield();
		}
	}

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/RenderBuffers;)Lnet/minecraft/client/renderer/LevelRenderer;"))
	private LevelRenderer onWorldRendererNew(Minecraft client, RenderBuffers bufferBuilders) {
		return new CanvasWorldRenderer(client, bufferBuilders);
	}

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "(II)Lcom/mojang/blaze3d/pipeline/MainTarget;"))
	private MainTarget onFrameBufferNew(int width, int height) {
		return new PrimaryFrameBuffer(width, height);
	}

	@Override
	public ItemColors canvas_itemColors() {
		return itemColors;
	}
}
