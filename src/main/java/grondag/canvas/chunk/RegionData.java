package grondag.canvas.chunk;

import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class RegionData {
	public static final RegionData EMPTY = new RegionData() {
		@Override
		public boolean isVisibleThrough(Direction direction, Direction direction2) {
			return false;
		}
	};

	final ObjectOpenHashSet<RenderLayer> nonEmptyLayers = new ObjectOpenHashSet<>();
	final ObjectOpenHashSet<RenderLayer> initializedLayers = new ObjectOpenHashSet<>();
	final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	ChunkOcclusionData occlusionGraph = new ChunkOcclusionData();
	boolean empty = true;
	@Nullable BufferBuilder.State bufferState;

	public boolean isEmpty() {
		return empty;
	}

	public boolean isEmpty(RenderLayer renderLayer) {
		return !nonEmptyLayers.contains(renderLayer);
	}

	public List<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	public boolean isVisibleThrough(Direction direction, Direction direction2) {
		return occlusionGraph.isVisibleThrough(direction, direction2);
	}

	public boolean markInitialized(RenderLayer renderLayer) {
		return initializedLayers.add(renderLayer);
	}

	public void markPopulated(RenderLayer renderLayer) {
		empty = false;
		nonEmptyLayers.add(renderLayer);
	}

	public void endBuffering(float x, float y, float z, BlockBufferBuilderStorage buffers) {
		final RenderLayer translucent = RenderLayer.getTranslucent();

		if (nonEmptyLayers.contains(translucent)) {
			final BufferBuilder buffer = buffers.get(RenderLayer.getTranslucent());
			buffer.sortQuads(x, y, z);
			bufferState = buffer.popState();
		}

		for(final RenderLayer layer : initializedLayers) {
			buffers.get(layer).end();
		}
	}

	public void setOcclusionGraph(ChunkOcclusionData occlusionGraph) {
		this.occlusionGraph = occlusionGraph;
	}
}