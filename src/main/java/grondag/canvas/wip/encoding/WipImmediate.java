package grondag.canvas.wip.encoding;

import java.util.Map;
import java.util.SortedMap;

import grondag.canvas.mixinterface.MultiPhaseExt;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.util.Util;

public class WipImmediate extends Immediate {
	private final WipVertexCollectorList collectors = new WipVertexCollectorList();

	protected WipImmediate(BufferBuilder fallbackBuffer, Map<RenderLayer, BufferBuilder> layerBuffers) {
		super(fallbackBuffer, layerBuffers);
	}

	@Override
	public VertexConsumer getBuffer(RenderLayer renderLayer) {
		final WipVertexCollector result = collectors.get(((MultiPhaseExt) renderLayer).canvas_renderState());

		if (result == null) {
			return super.getBuffer(renderLayer);
		} else {
			result.vertexState(((MultiPhaseExt) renderLayer).canvas_vertexState());
			return result;
		}
	}

	@Override
	public void draw() {
		final int limit = collectors.size();

		for (int i = 0; i < limit; ++i) {
			final WipVertexCollectorImpl collector = collectors.get(i);
			collector.drawAndClear();
		}

		collectors.clear();
		super.draw();
	}

	@Override
	public void draw(RenderLayer layer) {
		final WipVertexCollectorImpl collector = collectors.getIfExists(((MultiPhaseExt) layer).canvas_renderState());

		if (collector == null) {
			super.draw(layer);
		} else {
			collector.drawAndClear();
		}
	}

	// WIP: need this?
	private static final BlockBufferBuilderStorage blockBuilders = new BlockBufferBuilderStorage();

	// WIP: should not need all these
	private static final SortedMap<RenderLayer, BufferBuilder> entityBuilders = Util.make(new Object2ObjectLinkedOpenHashMap<>(), (object2ObjectLinkedOpenHashMap) -> {
		object2ObjectLinkedOpenHashMap.put(TexturedRenderLayers.getEntitySolid(), blockBuilders.get(RenderLayer.getSolid()));
		object2ObjectLinkedOpenHashMap.put(TexturedRenderLayers.getEntityCutout(), blockBuilders.get(RenderLayer.getCutout()));
		object2ObjectLinkedOpenHashMap.put(TexturedRenderLayers.getBannerPatterns(), blockBuilders.get(RenderLayer.getCutoutMipped()));
		object2ObjectLinkedOpenHashMap.put(TexturedRenderLayers.getEntityTranslucentCull(), blockBuilders.get(RenderLayer.getTranslucent()));
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, TexturedRenderLayers.getShieldPatterns());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, TexturedRenderLayers.getBeds());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, TexturedRenderLayers.getShulkerBoxes());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, TexturedRenderLayers.getSign());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, TexturedRenderLayers.getChest());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getTranslucentNoCrumbling());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getArmorGlint());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getArmorEntityGlint());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getGlint());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getGlintDirect());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.method_30676());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getEntityGlint());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getEntityGlintDirect());
		assignBufferBuilder(object2ObjectLinkedOpenHashMap, RenderLayer.getWaterMask());
		ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach((renderLayer) -> {
			assignBufferBuilder(object2ObjectLinkedOpenHashMap, renderLayer);
		});
	});

	private static void assignBufferBuilder(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferBuilder> builderStorage, RenderLayer layer) {
		builderStorage.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
	}

	public static final WipImmediate INSTANCE = new WipImmediate(new BufferBuilder(256), entityBuilders);


}
