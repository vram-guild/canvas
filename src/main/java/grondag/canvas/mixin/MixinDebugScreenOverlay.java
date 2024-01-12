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

import java.util.ArrayList;
//import java.util.List;

import com.google.common.collect.Lists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlUtil;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.BufferBuilder;
//import com.mojang.blaze3d.vertex.BufferUploader;
//import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexFormat;
//import com.mojang.math.Matrix4f;

//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
//import net.minecraft.client.renderer.GameRenderer;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.buffer.util.DirectBufferAllocator;
import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.light.color.LightDataManager;
import grondag.canvas.render.terrain.cluster.SlabAllocator;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.terrain.util.TerrainExecutor;
import grondag.canvas.varia.CanvasGlHelper;
//import grondag.canvas.config.Configurator;
//import grondag.canvas.mixinterface.BufferBuilderExt;
//import grondag.canvas.varia.AutoImmediate;

// WIP: restore or remove
@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay {
	//	@Shadow private Font font;
	//
	//	private List<String> leftList, rightList;
	//	private final BufferBuilder fillerBuffer = BufferBuilderExt.repeatableBuffer(0x1000);
	//	private final AutoImmediate immediate = new AutoImmediate();
	//	private long nextTime;
	//	private boolean rebuildLists = true;
	//
	//	private static final int HEIGHT = 9;
	//
	//	@Inject(method = "drawGameInformation", require = 1, cancellable = true, at = @At("HEAD"))
	//	private void beforeRenderLeftText(CallbackInfo ci) {
	//		if (Configurator.steadyDebugScreen) {
	//			final long time = System.currentTimeMillis();
	//
	//			if (time > nextTime) {
	//				rebuildLists = true;
	//				nextTime = time + 50;
	//			} else {
	//				ci.cancel();
	//			}
	//		} else {
	//			rebuildLists = true;
	//		}
	//	}
	//

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlUtil;getOpenGLVersion()Ljava/lang/String;"), require = 1)
	private String onGetGlDebugVersion() {
		return GlUtil.getOpenGLVersion() + " (OGL " + CanvasGlHelper.maxGlVersion() + " available)";
	}

	//
	//	@Redirect(method = "drawGameInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", remap = false), require = 1)
	//	private int onGetLeftListSize(List<String> leftList) {
	//		this.leftList = leftList;
	//		return 0;
	//	}
	//
	//	// Don't run at all if don't need to capture right list
	//	@Inject(method = "drawSystemInformation", require = 1, cancellable = true, at = @At("HEAD"))
	//	private void beforeRenderRightText(PoseStack matrixStack, CallbackInfo ci) {
	//		if (!rebuildLists) {
	//			// our remaining hooks don't get called if we cancel here, so draw if we cancel
	//			drawLists(matrixStack);
	//			ci.cancel();
	//		}
	//	}
	//
	//	// Capture right list and prevent draw
	//	@Redirect(method = "drawSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", remap = false), require = 1)
	//	private int onGetRightListSize(List<String> rightList) {
	//		this.rightList = rightList;
	//		return 0;
	//	}
	//
	//	@Inject(method = "drawSystemInformation", at = @At("RETURN"), cancellable = false, require = 1)
	//	private void afterRenderRightText(PoseStack matrixStack, CallbackInfo ci) {
	//		if (rebuildLists) {
	//			immediate.clear();
	//			fillerBuffer.discard();
	//			buildLists(matrixStack);
	//			rebuildLists = false;
	//		}
	//
	//		drawLists(matrixStack);
	//	}
	//
	//	private void drawLists(PoseStack matrixStack) {
	//		RenderSystem.enableBlend();
	//		RenderSystem.disableTexture();
	//		RenderSystem.defaultBlendFunc();
	//		RenderSystem.setShader(GameRenderer::getPositionColorShader);
	//		BufferUploader.end(fillerBuffer);
	//		RenderSystem.enableTexture();
	//		RenderSystem.disableBlend();
	//		immediate.drawRepeatable();
	//	}
	//
	//	private void buildLists(PoseStack matrixStack) {
	//		final Matrix4f matrix4f = matrixStack.last().pose();
	//		final Font textRenderer = this.font;
	//		final boolean rightToLeft = textRenderer.isBidirectional();
	//
	//		fillerBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
	//
	//		final int leftLimit = leftList.size();
	//
	//		for (int i = 0; i < leftLimit; ++i) {
	//			final String string = leftList.get(i);
	//
	//			if (string == null || string.isEmpty()) {
	//				continue;
	//			}
	//
	//			final int top = 2 + HEIGHT * i;
	//			final int x1 = 2 + textRenderer.width(string) + 1;
	//			final int y0 = top - 1;
	//			final int y1 = top + HEIGHT - 1;
	//			fillerBuffer.vertex(matrix4f, 1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, 1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//
	//			textRenderer.drawInBatch(string, 2.0F, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
	//		}
	//
	//		final int rightLimit = rightList.size();
	//		final int scaleWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 2;
	//
	//		for (int i = 0; i < rightLimit; ++i) {
	//			final String string = rightList.get(i);
	//
	//			if (string == null || string.isEmpty()) {
	//				continue;
	//			}
	//
	//			final int width = this.font.width(string);
	//			final int left = scaleWidth - width;
	//			final int top = 2 + HEIGHT * i;
	//			final int x0 = left - 1;
	//			final int x1 = left + width + 1;
	//			final int y0 = top - 1;
	//			final int y1 = top + HEIGHT - 1;
	//
	//			fillerBuffer.vertex(matrix4f, x0, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, x1, y1, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, x1, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//			fillerBuffer.vertex(matrix4f, x0, y0, 0.0F).color(0x50, 0x50, 0x50, 0x90).endVertex();
	//
	//			textRenderer.drawInBatch(string, left, top, 0xE0E0E0, false, matrix4f, immediate, false, 0, 0xF000F0, rightToLeft);
	//		}
	//
	//		fillerBuffer.end();
	//		leftList = null;
	//		rightList = null;
	//	}
	//

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false), require = 1)
	private ArrayList<String> onGetSystemInformation(Object[] elements) {
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

		result.add(LightDataManager.debugString());

		return result;
	}
}
