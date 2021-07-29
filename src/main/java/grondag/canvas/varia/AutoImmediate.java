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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;

import grondag.canvas.mixinterface.BufferBuilderExt;

/**
 * Meant primarily for text rendering, creates an Immediate vertex consumer
 * that automatically adds render layers as needed to avoid intermediate draw calls.
 */
public class AutoImmediate extends Immediate {
	public AutoImmediate() {
		super(null, new Object2ObjectOpenHashMap<>());
	}

	@Override
	public VertexConsumer getBuffer(RenderLayer renderLayer) {
		BufferBuilder builder = layerBuffers.get(renderLayer);

		if (builder == null) {
			builder = new BufferBuilder(renderLayer.getExpectedBufferSize());
			((BufferBuilderExt) builder).canvas_enableRepeatableDraw(true);
			layerBuffers.put(renderLayer, builder);
		}

		if (activeConsumers.add(builder)) {
			builder.begin(renderLayer.getDrawMode(), renderLayer.getVertexFormat());
		}

		return builder;
	}

	@Override
	public void drawCurrentLayer() {
		// NOOP because we will never use fallback layer
	}

	@Override
	public void draw() {
		drawRepeatable();
		clear();
	}

	@Override
	public void draw(RenderLayer renderLayer) {
		assert false : "Vanilla draw method not expected on AutoImmediate";

		BufferBuilder builder = layerBuffers.get(renderLayer);

		if (builder != null && activeConsumers.remove(builder)) {
			renderLayer.draw(builder, 0, 0, 0);
		}
	}

	public void drawRepeatable() {
		for (var layer : layerBuffers.keySet()) {
			drawRepeatable(layer);
		}
	}

	private void drawRepeatable(RenderLayer renderLayer) {
		BufferBuilder builder = layerBuffers.get(renderLayer);

		if (builder != null && activeConsumers.contains(builder)) {
			if (builder.isBuilding()) {
				builder.end();
			}

			renderLayer.startDrawing();
			BufferRenderer.draw(builder);
			renderLayer.endDrawing();
		}
	}

	public void clear() {
		for (var buffer : activeConsumers) {
			buffer.reset();
		}

		activeConsumers.clear();
	}
}
