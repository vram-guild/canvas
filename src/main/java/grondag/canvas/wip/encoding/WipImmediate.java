package grondag.canvas.wip.encoding;

import java.util.Map;
import java.util.SortedMap;

import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.canvas.wip.state.RenderContextState;
import grondag.canvas.wip.state.WipRenderState;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.util.Util;

public class WipImmediate extends Immediate {
	private final WipVertexCollectorList collectors;

	private final ObjectArrayList<WipVertexCollectorImpl> drawList = new ObjectArrayList<>();

	public WipImmediate(BufferBuilder fallbackBuffer, Map<RenderLayer, BufferBuilder> layerBuffers, RenderContextState contextState) {
		super(fallbackBuffer, layerBuffers);
		collectors = new WipVertexCollectorList(contextState);
	}

	@Override
	public VertexConsumer getBuffer(RenderLayer renderLayer) {
		final WipVertexCollector result = collectors.get(((MultiPhaseExt) renderLayer).canvas_renderState());

		if (result == null) {
			assert WipRenderState.isExcluded(renderLayer) : "Unable to retrieve vertex collector for non-excluded render layer";

			return super.getBuffer(renderLayer);
		} else {
			result.vertexState(((MultiPhaseExt) renderLayer).canvas_vertexState());
			return result;
		}
	}

	public void drawCollectors(boolean translucentTerrain) {
		final ObjectArrayList<WipVertexCollectorImpl> drawList = this.drawList;
		final int limit = collectors.size();

		if (limit != 0) {
			for (int i = 0; i < limit; ++i) {
				final WipVertexCollectorImpl collector = collectors.get(i);

				if (collector.materialState.isTranslucentTerrain == translucentTerrain && !collector.isEmpty()) {
					drawList.add(collector);
				}
			}
		}

		WipVertexCollectorImpl.drawAndClear(drawList);
	}

	@Override
	public void draw() {
		final ObjectArrayList<WipVertexCollectorImpl> drawList = this.drawList;
		final int limit = collectors.size();

		for (int i = 0; i < limit; ++i) {
			final WipVertexCollectorImpl collector = collectors.get(i);

			if (!collector.isEmpty()) {
				drawList.add(collector);
			}
		}

		WipVertexCollectorImpl.drawAndClear(drawList);
		collectors.clear();
		super.draw();
	}

	@Override
	public void draw(RenderLayer layer) {
		if (WipRenderState.isExcluded(layer)) {
			super.draw(layer);
		} else {
			final WipVertexCollectorImpl collector = collectors.getIfExists(((MultiPhaseExt) layer).canvas_renderState());

			if (collector != null) {
				collector.drawAndClear();
			}
		}
	}

	public static SortedMap<RenderLayer, BufferBuilder> entityBuilders() {
		return Util.make(new Object2ObjectLinkedOpenHashMap<>(), (object2ObjectLinkedOpenHashMap) -> {

			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getTranslucentNoCrumbling());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getArmorGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getArmorEntityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getDirectGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.method_30676());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getEntityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getDirectEntityGlint());
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getWaterMask());
			ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
				assignBufferBuilder(object2ObjectLinkedOpenHashMap, renderLayer);
			});
		});
	}

	private static void assignBufferBuilder(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferBuilder> builderStorage, RenderLayer layer) {
		builderStorage.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
	}
}
