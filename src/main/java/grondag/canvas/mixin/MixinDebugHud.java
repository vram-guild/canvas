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
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.buffer.util.DirectBufferAllocator;
import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.config.Configurator;
import grondag.canvas.config.TerrainRenderConfigOption;
import grondag.canvas.mixinterface.BufferBuilderExt;
import grondag.canvas.render.terrain.cluster.SlabAllocator;
import grondag.canvas.render.terrain.cluster.VertexClusterRealm;
import grondag.canvas.terrain.util.TerrainExecutor;
import grondag.canvas.varia.AutoImmediate;

@Mixin(DebugHud.class)
public class MixinDebugHud extends DrawableHelper {
	@Shadow private TextRenderer textRenderer;

	private List<String> leftList, rightList;
	private final BufferBuilder fillerBuffer = BufferBuilderExt.repeatableBuffer(0x1000);
	private final AutoImmediate immediate = new AutoImmediate();
	private long nextTime;
	private boolean rebuildLists = true;

	private static final int HEIGHT = 9;

	@Inject(method = "renderLeftText", require = 1, cancellable = true, at = @At("HEAD"))
	private void beforeRenderLeftText(CallbackInfo ci) {
		if (Configurator.steadyDebugScreen) {
			final long time = System.currentTimeMillis();

			if (time > nextTime) {
				rebuildLists = true;
				nextTime = time + 50;
			} else {
				ci.cancel();
			}
		} else {
			rebuildLists = true;
		}
	}

	@Redirect(method = "renderLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int onGetLeftListSize(List<String> leftList) {
		this.leftList = leftList;
		return 0;
	}

	@Inject(method = "renderRightText", require = 1, cancellable = true, at = @At("HEAD"))
	private void beforeRenderRightText(MatrixStack matrixStack, CallbackInfo ci) {
		if (!rebuildLists) {
			drawLists(matrixStack);
			ci.cancel();
		}
	}

	@Redirect(method = "renderRightText", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int onGetRightListSize(List<String> rightList) {
		this.rightList = rightList;
		return 0;
	}

	@Inject(method = "renderRightText", at = @At("RETURN"), cancellable = false, require = 1)
	private void afterRenderRightText(MatrixStack matrixStack, CallbackInfo ci) {
		if (rebuildLists) {
			immediate.clear();
			fillerBuffer.reset();
			buildLists(matrixStack);
			rebuildLists = false;
			drawLists(matrixStack);
		}
	}

	private void drawLists(MatrixStack matrixStack) {
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferRenderer.draw(fillerBuffer);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		immediate.drawRepeatable();
	}

	private void buildLists(MatrixStack matrixStack) {
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

			textRenderer.draw(string, 2.0F, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
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

			textRenderer.draw(string, left, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
		}

		fillerBuffer.end();
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
		result.add(TransferBuffers.debugString());
		result.add(ArrayVertexCollector.debugReport());
		TerrainExecutor.INSTANCE.debugReport(result);

		if (Configurator.terrainRenderConfigOption == TerrainRenderConfigOption.CLUSTERED) {
			result.add("Solid " + VertexClusterRealm.SOLID.debugSummary());
			result.add("Translucent " + VertexClusterRealm.TRANSLUCENT.debugSummary());
			result.add(SlabAllocator.debugSummary());
		}

		return result;
	}
}
