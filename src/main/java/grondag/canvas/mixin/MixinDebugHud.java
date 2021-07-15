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

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.DirectBufferAllocator;
import grondag.canvas.buffer.GlBufferAllocator;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.terrain.util.TerrainExecutor;

@Mixin(DebugHud.class)
public class MixinDebugHud extends DrawableHelper {
	@Shadow private TextRenderer textRenderer;

	private List<String> leftList, rightList;

	@Redirect(method = "renderLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int onGetLeftListSize(List<String> leftList) {
		this.leftList = leftList;
		return 0;
	}

	@Redirect(method = "renderRightText", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int onGetRightListSize(List<String> rightList) {
		this.rightList = rightList;
		return 0;
	}

	@Inject(method = "renderRightText", at = @At("RETURN"), cancellable = false, require = 1)
	private void afterRenderRightText(MatrixStack matrixStack, CallbackInfo ci) {
		drawLists(matrixStack);
	}

	private final BufferBuilder fillerBuffer = new BufferBuilder(4096);
	private final VertexConsumerProvider.Immediate textBuffer = VertexConsumerProvider.immediate(new BufferBuilder(4096));
	private static final int HEIGHT = 9;

	private void drawLists(MatrixStack matrixStack) {
		final Matrix4f matrix4f = matrixStack.peek().getModel();
		final TextRenderer textRenderer = this.textRenderer;
		final boolean rightToLeft = textRenderer.isRightToLeft();

		fillerBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		final int leftLimit = leftList.size();

		for (int i = 0; i < leftLimit; ++i) {
			final String string = leftList.get(i);

			if (string == null || string.isEmpty()) {
				continue;
			}

			final int top = 2 + HEIGHT * i;
			final int x1 = 2 + textRenderer.getWidth(string) + 1;
			final int y0 = top - 1;
			final int y1 = top + HEIGHT - 1;
			fillerBuffer.vertex(matrix4f, 1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, 1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();

			textRenderer.draw(string, 2.0F, top, 0xE0E0E0, false, matrix4f, textBuffer, false, 0, 0xF000F0, rightToLeft);
		}

		final int rightLimit = rightList.size();
		final int scaleWidth = MinecraftClient.getInstance().getWindow().getScaledWidth() - 2;

		for (int i = 0; i < rightLimit; ++i) {
			final String string = rightList.get(i);

			if (string == null || string.isEmpty()) {
				continue;
			}

			final int width = this.textRenderer.getWidth(string);
			final int left = scaleWidth - width;
			final int top = 2 + HEIGHT * i;
			final int x0 = left - 1;
			final int x1 = left + width + 1;
			final int y0 = top - 1;
			final int y1 = top + HEIGHT - 1;

			fillerBuffer.vertex(matrix4f, x0, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();
			fillerBuffer.vertex(matrix4f, x0, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).next();

			textRenderer.draw(string, left, top, 0xE0E0E0, false, matrix4f, textBuffer, false, 0, 0xF000F0, rightToLeft);
		}

		fillerBuffer.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferRenderer.draw(fillerBuffer);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		textBuffer.draw();

		leftList = null;
		rightList = null;
	}

	@Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
	private ArrayList<String> onGetRightText(Object[] elements) {
		ArrayList<String> result = Lists.newArrayList((String[]) elements);
		result.add("");
		result.add("Canvas Renderer " + CanvasMod.versionString);
		result.add(DirectBufferAllocator.debugString());
		result.add(GlBufferAllocator.debugString());
		result.add(ArrayVertexCollector.debugReport());
		TerrainExecutor.INSTANCE.debugReport(result);
		return result;
	}
}
