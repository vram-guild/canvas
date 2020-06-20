package grondag.canvas.buffer.packing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial.DrawableMaterial;
import grondag.canvas.chunk.UploadableChunk;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialState;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.ShaderContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VertexCollectorList {
	private final VertexCollectorImpl[] collectors = new VertexCollectorImpl[MaterialState.MAX_MATERIAL_STATES];

	private final BufferPackingList packingList = new BufferPackingList();

	private int usedCount = 0;

	private final ObjectArrayList<VertexCollectorImpl> allCollectors = new ObjectArrayList<>();


	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		for (int i = 0; i < usedCount; i++) {
			allCollectors.get(i).clear();
		}

		usedCount = 0;
		System.arraycopy(EMPTY, 0, collectors, 0, MaterialState.MAX_MATERIAL_STATES);
	}

	public final boolean isEmpty() {
		if (usedCount == 0) {
			return true;
		}

		final int usedCount = this.usedCount;

		for(int i = 0; i < usedCount; i++) {
			if (allCollectors.get(i).integerSize() > 0) {
				return false;
			}
		}

		return true;
	}

	public final VertexCollectorImpl getIfExists(MaterialState materialState) {
		return collectors[materialState.index];
	}

	public final VertexCollectorImpl get(MaterialState materialState) {
		final int materialIndex = materialState.index;
		VertexCollectorImpl result = collectors[materialIndex];

		if(result == null) {
			result = emptyCollector().prepare(materialState);
			collectors[materialIndex] = result;
		}

		return result;
	}

	private VertexCollectorImpl emptyCollector() {
		VertexCollectorImpl result;

		if(usedCount == allCollectors.size()) {
			result = new VertexCollectorImpl(this);
			allCollectors.add(result);
		} else {
			result = allCollectors.get(usedCount);
		}

		usedCount++;
		return result;
	}

	public final void forEachExisting(Consumer<VertexCollectorImpl> consumer) {
		final int usedCount = this.usedCount;
		for(int i = 0; i < usedCount; i++) {
			consumer.accept(allCollectors.get(i));
		}
	}

	/**
	 * Sorts pipelines by pipeline index numerical order.
	 * DO NOT RETAIN A REFERENCE
	 */
	private final BufferPackingList packingListSolid() {
		final BufferPackingList packing = packingList;
		packing.clear();

		final int usedCount = this.usedCount;
		final Object[] collectors = allCollectors.elements();

		Arrays.sort(collectors, 0, usedCount, solidComparator);

		for (int i = 0; i < usedCount; i++) {
			final VertexCollectorImpl vertexCollector = (VertexCollectorImpl) collectors[i];
			final int vertexCount = vertexCollector.vertexCount();
			final MaterialState matState = vertexCollector.materialState();

			if (vertexCount != 0 && matState.shaderType != ShaderContext.Type.TRANSLUCENT) {
				packing.addPacking(matState, vertexCount);
			}
		}

		return packing;
	}

	public final UploadableChunk packUploadSolid() {
		final BufferPackingList packing = packingListSolid();

		// NB: for solid render, relying on pipelines being added to packing in
		// numerical order so that
		// all chunks can iterate pipelines independently while maintaining same
		// pipeline order within chunk
		return packing.size() == 0 ? null : new UploadableChunk(packing, this, MaterialVertexFormats.get(MaterialContext.TERRAIN, false));
	}

	/**
	 * Assumes only a single translucent material state - sorting is done externally
	 * DO NOT RETAIN A REFERENCE
	 */
	private final BufferPackingList packingListTranslucent(MaterialState translucentState) {
		final BufferPackingList packing = packingList;
		packing.clear();

		final VertexCollectorImpl vertexCollector = collectors[translucentState.index];

		if  (vertexCollector != null && vertexCollector.vertexCount() > 0) {
			packing.addPacking(translucentState, vertexCollector.vertexCount());
		}

		return packing;
	}

	public final UploadableChunk packUploadTranslucent(MaterialState translucentState) {
		final BufferPackingList packing = packingListTranslucent(translucentState);
		return packing.size() == 0 ? null : new UploadableChunk(packing, this, MaterialVertexFormats.get(MaterialContext.TERRAIN, true));
	}

	public int[][] getCollectorState(int[][] priorState) {
		int[][] result = priorState;

		final int usedCount = this.usedCount;

		if (result == null || result.length != usedCount) {
			result = new int[usedCount][0];
		}

		for(int i = 0; i < usedCount; i++) {
			result[i] = allCollectors.get(i).saveState(result[i]);
		}

		return result;
	}

	public void loadCollectorState(int[][] stateData) {
		clear();
		for (final int[] data : stateData) {
			final VertexCollectorImpl vc = emptyCollector().loadState(data);
			collectors[vc.materialState().index] = vc;
		}
	}

	private static final Comparator<Object> solidComparator = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return Long.compare(((VertexCollectorImpl)o1).materialState().sortIndex, ((VertexCollectorImpl)o2).materialState().sortIndex);
		}
	};

	private static final VertexCollectorImpl[] EMPTY = new VertexCollectorImpl[MaterialState.MAX_MATERIAL_STATES];

	public VertexCollectorImpl getDirect(MaterialContext context, DrawableMaterial material) {
		return get(MaterialState.get(context, material));
	}

	public boolean contains(MaterialState materialState) {
		return collectors[materialState.index] != null;
	}
}
