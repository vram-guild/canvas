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

package grondag.canvas.buffer.input;

import java.util.Map;
import java.util.SortedMap;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.mesh.FrexVertexConsumer;
import io.vram.frex.api.mesh.FrexVertexConsumerProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelBakery;

import grondag.canvas.buffer.util.DrawableStream;
import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.MultiPhaseExt;

public class CanvasImmediate extends BufferSource implements FrexVertexConsumerProvider {
	public final VertexCollectorList collectors = new VertexCollectorList(false);
	public final RenderContextState contextState;

	public CanvasImmediate(BufferBuilder fallbackBuffer, Map<RenderType, BufferBuilder> layerBuffers, RenderContextState contextState) {
		super(fallbackBuffer, layerBuffers);
		this.contextState = contextState;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderLayer) {
		RenderMaterialImpl mat = ((MultiPhaseExt) renderLayer).canvas_materialState();

		if (mat == RenderMaterialImpl.MISSING) {
			return super.getBuffer(renderLayer);
		}

		mat = contextState.mapMaterial(mat);

		if (mat == RenderMaterialImpl.MISSING) {
			return super.getBuffer(renderLayer);
		} else {
			return collectors.consumer.prepare(mat);
		}
	}

	@Override
	public FrexVertexConsumer getConsumer(RenderMaterial material) {
		final RenderMaterialImpl mat = contextState.mapMaterial((RenderMaterialImpl) material);
		return collectors.consumer.prepare(mat);
	}

	public DrawableStream prepareDrawable(MaterialTarget target) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectors.sortedDrawList(target);

		return drawList.isEmpty() ? DrawableStream.EMPTY : new DrawableStream(drawList);
	}

	public void drawCollectors(MaterialTarget target) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectors.sortedDrawList(target);

		if (!drawList.isEmpty()) {
			ArrayVertexCollector.draw(drawList);
		}
	}

	@Override
	public void endBatch() {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectors.sortedDrawList(Predicates.alwaysTrue());

		if (!drawList.isEmpty()) {
			ArrayVertexCollector.draw(drawList);
		}

		super.endBatch();
	}

	@Override
	public void endBatch(RenderType layer) {
		if (RenderLayerHelper.isExcluded(layer)) {
			super.endBatch(layer);
		} else {
			final ArrayVertexCollector collector = collectors.getIfExists(((MultiPhaseExt) layer).canvas_materialState());

			if (collector != null && !collector.isEmpty()) {
				collector.draw(true);
			}
		}
	}

	public static SortedMap<RenderType, BufferBuilder> entityBuilders() {
		return Util.make(new Object2ObjectLinkedOpenHashMap<>(), (object2ObjectLinkedOpenHashMap) -> {
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.translucentNoCrumbling());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.armorGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.armorEntityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glintDirect());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.glintTranslucent());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.entityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.entityGlintDirect());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderType.waterMask());
			ModelBakery.DESTROY_TYPES.forEach((renderLayer) -> {
				assignBufferBuilder(object2ObjectLinkedOpenHashMap, renderLayer);
			});
		});
	}

	private static void assignBufferBuilder(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> builderStorage, RenderType layer) {
		builderStorage.put(layer, new BufferBuilder(layer.bufferSize()));
	}
}
