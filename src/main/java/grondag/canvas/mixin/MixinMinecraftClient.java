/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.thread.ReentrantThreadExecutor;

import grondag.canvas.Configurator;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.varia.CanvasGlHelper;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> implements MinecraftClientExt {
	@Shadow ItemColors itemColors;

	protected MixinMinecraftClient(String dummy) {
		super(dummy);
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void hookInit(CallbackInfo info) {
		CanvasGlHelper.init();
	}

	@Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"), method = "render", require = 1, allow = 1)
	private void onYield() {
		if (!Configurator.greedyRenderThread) {
			Thread.yield();
		}
	}

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/render/BufferBuilderStorage;)Lnet/minecraft/client/render/WorldRenderer;"))
	private WorldRenderer onWorldRendererNew(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		return new CanvasWorldRenderer(client, bufferBuilders);
	}

	@Override
	public ItemColors canvas_itemColors() {
		return itemColors;
	}
}
