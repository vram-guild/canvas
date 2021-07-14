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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.buffer.GlBufferAllocator;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.mixinterface.TextRendererExt;
import grondag.canvas.render.gui.DrawBetterer;

@Mixin(DebugHud.class)
public class MixinDebugHud extends DrawableHelper {
	@Shadow private TextRenderer textRenderer;

	@Inject(method = "getLeftText", at = @At("RETURN"), cancellable = false, require = 1)
	private void onGetLeftText(CallbackInfoReturnable<List<String>> ci) {
		final List<String> list = ci.getReturnValue();

		// if (Configurator.hdLightmaps()) {
		// 	list.add("HD Lightmap Occupancy: " + LightmapHd.occupancyReport());
		// }

		list.add(TransferBufferAllocator.debugString());
		list.add(GlBufferAllocator.debugString());
		list.add(ArrayVertexCollector.debugReport());
	}

	@Inject(method = "renderLeftText", at = @At("HEAD"), cancellable = false, require = 1)
	private void beforeRenderLeftText(MatrixStack matrixStack, CallbackInfo ci) {
		final Matrix4f matrix4f = matrixStack.peek().getModel();
		DrawBetterer.beginBatchFill(matrix4f);
		((TextRendererExt) textRenderer).canvas_beginBatchDraw(matrix4f);
	}

	@Inject(method = "renderRightText", at = @At("RETURN"), cancellable = false, require = 1)
	private void afterRenderRightText(MatrixStack matrixStack, CallbackInfo ci) {
		DrawBetterer.endBatchFile();
		((TextRendererExt) textRenderer).canvas_endBatchDraw();
	}
}
