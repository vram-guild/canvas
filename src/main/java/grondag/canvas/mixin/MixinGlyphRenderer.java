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

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import grondag.canvas.mixinterface.BufferBuilderExt;
import grondag.canvas.mixinterface.Matrix4fExt;

@Mixin(GlyphRenderer.class)
public abstract class MixinGlyphRenderer {
	@Shadow private float minU;
	@Shadow private float maxU;
	@Shadow private float minV;
	@Shadow private float maxV;
	@Shadow private float minX;
	@Shadow private float maxX;
	@Shadow private float minY;
	@Shadow private float maxY;

	private static final Vec3f pos = new Vec3f();
	// NB: size in bytes is size of integer array for whole quad
	private static final int[] quadData = new int[VertexFormats.POSITION_COLOR_TEXTURE_LIGHT.getVertexSize()];

	// PERF: consider handling drawRectangle also

	/**
	 * @author grondag
	 * @reason performance; calls are too frequent and parameter stack too large for inject to perform well
	 */
	@Overwrite
	public void draw(boolean oblique, float x, float y, Matrix4f matrix4f, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int lightmap) {
		final float x0 = x + minX;
		final float x1 = x + maxX;
		final float y0 = minY - 3.0F;
		final float y1 = maxY - 3.0F;
		final float top = y + y0;
		final float bottom = y + y1;
		final float obqTop = oblique ? 1.0F - 0.25F * y0 : 0.0F;
		final float obqBotom = oblique ? 1.0F - 0.25F * y1 : 0.0F;

		if (vertexConsumer instanceof BufferBuilderExt extBuilder
				&& extBuilder.canvas_canSupportDirect(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT)
				&& RenderSystem.isOnRenderThread() // This last is because we are using static vars
		) {
			final Matrix4fExt matrix = Matrix4fExt.cast(matrix4f);
			final int color = (int) (red * 255.0F) | ((int) (green * 255.0F) << 8) | ((int) (blue * 255.0F) << 16) | ((int) (alpha * 255.0F) << 24);
			int i = 0;

			pos.set(x0 + obqTop, top, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.getX());
			quadData[i++] = Float.floatToRawIntBits(pos.getY());
			quadData[i++] = Float.floatToRawIntBits(pos.getZ());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(minU);
			quadData[i++] = Float.floatToRawIntBits(minV);
			quadData[i++] = lightmap;

			pos.set(x0 + obqBotom, bottom, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.getX());
			quadData[i++] = Float.floatToRawIntBits(pos.getY());
			quadData[i++] = Float.floatToRawIntBits(pos.getZ());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(minU);
			quadData[i++] = Float.floatToRawIntBits(maxV);
			quadData[i++] = lightmap;

			pos.set(x1 + obqBotom, bottom, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.getX());
			quadData[i++] = Float.floatToRawIntBits(pos.getY());
			quadData[i++] = Float.floatToRawIntBits(pos.getZ());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(maxU);
			quadData[i++] = Float.floatToRawIntBits(maxV);
			quadData[i++] = lightmap;

			pos.set(x1 + obqTop, top, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.getX());
			quadData[i++] = Float.floatToRawIntBits(pos.getY());
			quadData[i++] = Float.floatToRawIntBits(pos.getZ());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(maxU);
			quadData[i++] = Float.floatToRawIntBits(minV);
			quadData[i++] = lightmap;

			assert i == quadData.length;
			extBuilder.canvas_putQuadDirect(quadData);
		} else {
			vertexConsumer.vertex(matrix4f, x0 + obqTop, top, 0.0F).color(red, green, blue, alpha).texture(minU, minV).light(lightmap).next();
			vertexConsumer.vertex(matrix4f, x0 + obqBotom, bottom, 0.0F).color(red, green, blue, alpha).texture(minU, maxV).light(lightmap).next();
			vertexConsumer.vertex(matrix4f, x1 + obqBotom, bottom, 0.0F).color(red, green, blue, alpha).texture(maxU, maxV).light(lightmap).next();
			vertexConsumer.vertex(matrix4f, x1 + obqTop, top, 0.0F).color(red, green, blue, alpha).texture(maxU, minV).light(lightmap).next();
		}
	}
}
