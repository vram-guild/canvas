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
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.GameRenderer;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.buffer.util.DirectBufferAllocator;
import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.BufferBuilderExt;
import grondag.canvas.render.terrain.cluster.SlabAllocator;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.terrain.util.TerrainExecutor;
import grondag.canvas.varia.AutoImmediate;
import grondag.canvas.varia.CanvasGlHelper;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugHud extends GuiComponent {
	@Shadow private Font textRenderer;

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

	@Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlDebugInfo;getVersion()Ljava/lang/String;"))
	private String onGetGlDebugVersion() {
		return GlUtil.getOpenGLVersion() + " (OGL " + CanvasGlHelper.maxGlVersion() + " available)";
	}

	@Redirect(method = "renderLeftText", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int onGetLeftListSize(List<String> leftList) {
		this.leftList = leftList;
		return 0;
	}

	@Inject(method = "renderRightText", require = 1, cancellable = true, at = @At("HEAD"))
	private void beforeRenderRightText(PoseStack matrixStack, CallbackInfo ci) {
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
	private void afterRenderRightText(PoseStack matrixStack, CallbackInfo ci) {
		if (rebuildLists) {
			immediate.clear();
			fillerBuffer.discard();
			buildLists(matrixStack);
			rebuildLists = false;
			drawLists(matrixStack);
		}
	}

	private void drawLists(PoseStack matrixStack) {
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferUploader.end(fillerBuffer);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		immediate.drawRepeatable();
	}

	private void buildLists(PoseStack matrixStack) {
		final Matrix4f matrix4f = matrixStack.last().pose();
		final Font textRenderer = this.textRenderer;
		final boolean rightToLeft = textRenderer.isBidirectional();

		fillerBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		final int leftLimit = leftList.size();

		for (int i = 0; i < leftLimit; ++i) {
			final String string = leftList.get(i);

			if (string == null || string.isEmpty()) {
				continue;
			}

			final int top = 2 + HEIGHT * i;
			final int x1 = 2 + textRenderer.width(string) + 1;
			final int y0 = top - 1;
			final int y1 = top + HEIGHT - 1;
			fillerBuffer.vertex(matrix4f, 1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, 1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();

			textRenderer.drawInBatch(string, 2.0F, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
		}

		final int rightLimit = rightList.size();
		final int scaleWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 2;

		for (int i = 0; i < rightLimit; ++i) {
			final String string = rightList.get(i);

			if (string == null || string.isEmpty()) {
				continue;
			}

			final int width = this.textRenderer.width(string);
			final int left = scaleWidth - width;
			final int top = 2 + HEIGHT * i;
			final int x0 = left - 1;
			final int x1 = left + width + 1;
			final int y0 = top - 1;
			final int y1 = top + HEIGHT - 1;

			fillerBuffer.vertex(matrix4f, x0, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
			fillerBuffer.vertex(matrix4f, x0, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();

			textRenderer.drawInBatch(string, left, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
		}

		fillerBuffer.end();
		leftList = null;
		rightList = null;
	}

	@Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
	private ArrayList<String> onGetRightText(Object[] elements) {
		final ArrayList<String> result = Lists.newArrayList((String[]) elements);
		result.add("");
		result.add("Canvas Renderer " + CanvasMod.versionString);
		result.add(DirectBufferAllocator.debugString());
		result.add(GlBufferAllocator.debugString());
		result.add(TransferBuffers.debugString());
		result.add(ArrayVertexCollector.debugReport());
		TerrainExecutor.INSTANCE.debugReport(result);

		@SuppressWarnings("resource")
		final var worldRenderState = CanvasWorldRenderer.instance().worldRenderState;
		result.add("Solid " + worldRenderState.solidClusterRealm.debugSummary());
		result.add("Translucent " + worldRenderState.translucentClusterRealm.debugSummary());
		result.add(worldRenderState.drawlistDebugSummary());
		result.add(SlabAllocator.debugSummary());

		return result;
	}
}
