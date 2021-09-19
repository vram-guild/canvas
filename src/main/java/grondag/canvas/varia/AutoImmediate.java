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

package grondag.canvas.varia;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;

import grondag.canvas.mixinterface.BufferBuilderExt;

/**
 * Meant primarily for text rendering, creates an Immediate vertex consumer
 * that automatically adds render layers as needed to avoid intermediate draw calls.
 */
public class AutoImmediate extends BufferSource {
	public AutoImmediate() {
		super(null, new Object2ObjectOpenHashMap<>());
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderLayer) {
		BufferBuilder builder = fixedBuffers.get(renderLayer);

		if (builder == null) {
			builder = new BufferBuilder(renderLayer.bufferSize());
			((BufferBuilderExt) builder).canvas_enableRepeatableDraw(true);
			fixedBuffers.put(renderLayer, builder);
		}

		if (startedBuffers.add(builder)) {
			builder.begin(renderLayer.mode(), renderLayer.format());
		}

		return builder;
	}

	@Override
	public void endLastBatch() {
		// NOOP because we will never use fallback layer
	}

	@Override
	public void endBatch() {
		drawRepeatable();
		clear();
	}

	@Override
	public void endBatch(RenderType renderLayer) {
		assert false : "Vanilla draw method not expected on AutoImmediate";

		final BufferBuilder builder = fixedBuffers.get(renderLayer);

		if (builder != null && startedBuffers.remove(builder)) {
			renderLayer.end(builder, 0, 0, 0);
		}
	}

	public void drawRepeatable() {
		for (final var layer : fixedBuffers.keySet()) {
			drawRepeatable(layer);
		}
	}

	private void drawRepeatable(RenderType renderLayer) {
		final BufferBuilder builder = fixedBuffers.get(renderLayer);

		if (builder != null && startedBuffers.contains(builder)) {
			if (builder.building()) {
				builder.end();
			}

			renderLayer.setupRenderState();
			BufferUploader.end(builder);
			renderLayer.clearRenderState();
		}
	}

	public void clear() {
		for (final var buffer : startedBuffers) {
			buffer.discard();
		}

		startedBuffers.clear();
	}
}
