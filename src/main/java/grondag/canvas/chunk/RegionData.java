package grondag.canvas.chunk;

import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.material.MaterialContext;

@Environment(EnvType.CLIENT)
public class RegionData {
	public static final RegionData EMPTY = new RegionData() {
		@Override
		public boolean isVisibleThrough(Direction direction, Direction direction2) {
			return false;
		}
	};

	//final ObjectOpenHashSet<RenderLayer> nonEmptyLayers = new ObjectOpenHashSet<>();
	//final ObjectOpenHashSet<RenderLayer> initializedLayers = new ObjectOpenHashSet<>();
	final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	ChunkOcclusionData occlusionGraph = new ChunkOcclusionData();
	@Nullable int[] translucentState;

	//	public boolean isEmpty(RenderLayer renderLayer) {
	//		return !nonEmptyLayers.contains(renderLayer);
	//	}

	public List<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	public boolean isVisibleThrough(Direction direction, Direction direction2) {
		return occlusionGraph.isVisibleThrough(direction, direction2);
	}

	//	public boolean markInitialized(RenderLayer renderLayer) {
	//		return initializedLayers.add(renderLayer);
	//	}
	//
	//	public void markPopulated(RenderLayer renderLayer) {
	//		empty = false;
	//		nonEmptyLayers.add(renderLayer);
	//	}

	public void endBuffering(float x, float y, float z, VertexCollectorList buffers) {
		final RenderLayer translucent = RenderLayer.getTranslucent();

		if (buffers.contains(MaterialContext.TERRAIN, translucent)) {
			final VertexCollectorImpl buffer = buffers.get(MaterialContext.TERRAIN, translucent);
			buffer.sortQuads(x, y, z);
			translucentState = buffer.saveState(translucentState);
		}
	}

	public void setOcclusionGraph(ChunkOcclusionData occlusionGraph) {
		this.occlusionGraph = occlusionGraph;
	}
}