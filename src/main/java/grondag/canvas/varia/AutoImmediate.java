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

package grondag.canvas.varia;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;

import grondag.canvas.mixinterface.BufferBuilderExt;

/**
 * Meant primarily for text rendering, creates an Immediate vertex consumer
 * that automatically adds render layers as needed to avoid intermediate draw calls.
 */
public class AutoImmediate /*extends BufferSource*/ {
//	public AutoImmediate() {
//		super(null, new Object2ObjectOpenHashMap<>());
//	}
//
//	@Override
//	public VertexConsumer getBuffer(RenderType renderLayer) {
//		BufferBuilder builder = fixedBuffers.get(renderLayer);
//
//		if (builder == null) {
//			builder = new BufferBuilder(renderLayer.bufferSize());
//			((BufferBuilderExt) builder).canvas_enableRepeatableDraw(true);
//			fixedBuffers.put(renderLayer, builder);
//		}
//
//		if (startedBuffers.add(builder)) {
//			builder.begin(renderLayer.mode(), renderLayer.format());
//		}
//
//		return builder;
//	}
//
//	@Override
//	public void endLastBatch() {
//		// NOOP because we will never use fallback layer
//	}
//
//	@Override
//	public void endBatch() {
//		drawRepeatable();
//		clear();
//	}
//
//	@Override
//	public void endBatch(RenderType renderLayer) {
//		assert false : "Vanilla draw method not expected on AutoImmediate";
//
//		final BufferBuilder builder = fixedBuffers.get(renderLayer);
//
//		if (builder != null && startedBuffers.remove(builder)) {
//			renderLayer.end(builder, 0, 0, 0);
//		}
//	}
//
//	public void drawRepeatable() {
//		for (final var layer : fixedBuffers.keySet()) {
//			drawRepeatable(layer);
//		}
//	}
//
//	private void drawRepeatable(RenderType renderLayer) {
//		final BufferBuilder builder = fixedBuffers.get(renderLayer);
//
//		if (builder != null && startedBuffers.contains(builder)) {
//			if (builder.building()) {
//				builder.end();
//			}
//
//			renderLayer.setupRenderState();
//			BufferUploader.end(builder);
//			renderLayer.clearRenderState();
//		}
//	}
//
//	public void clear() {
//		for (final var buffer : startedBuffers) {
//			buffer.discard();
//		}
//
//		startedBuffers.clear();
//	}
}
