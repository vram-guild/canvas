package grondag.canvas.buffer.packing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.chunk.UploadableChunk;
import grondag.canvas.material.MaterialState;

public class VertexCollectorList {
	private final AbstractVertexCollector[] collectors = new AbstractVertexCollector[MaterialState.MAX_MATERIAL_STATES];

	private final BufferPackingList packingList = new BufferPackingList();

	private int usedCount = 0;

	private final ObjectArrayList<AbstractVertexCollector> allCollectors = new ObjectArrayList<>();

	/** used in transparency layer sorting - updated with player eye coordinates */
	private double viewX;
	/** used in transparency layer sorting - updated with player eye coordinates */
	private double viewY;
	/** used in transparency layer sorting - updated with player eye coordinates */
	private double viewZ;

	/** used in transparency layer sorting - updated with origin of render cube */
	double renderOriginX = 0;
	/** used in transparency layer sorting - updated with origin of render cube */
	double renderOriginY = 0;
	/** used in transparency layer sorting - updated with origin of render cube */
	double renderOriginZ = 0;

	/**
	 * Releases any held vertex collectors and resets state
	 */
	public void clear() {
		renderOriginX = 0;
		renderOriginY = 0;
		renderOriginZ = 0;

		for (int i = 0; i < usedCount; i++) {
			allCollectors.get(i).clear();
		}

		usedCount = 0;
		System.arraycopy(EMPTY, 0, collectors, 0, MaterialState.MAX_MATERIAL_STATES);
	}

	public final boolean isEmpty() {
		return usedCount == 0;
	}

	/**
	 * Saves player eye coordinates for vertex sorting. Normally these will be the
	 * same values for every list but save in instance to stay consistent with
	 * Vanilla and possibly to support mods that do strange things with view entity
	 * perspective. (PortalGun?)
	 */
	public void setViewCoordinates(double x, double y, double z) {
		viewX = x;
		viewY = y;
		viewZ = z;
	}

	/**
	 * Called when a render chunk initializes buffer builders with offset.
	 */
	public void setRelativeRenderOrigin(double x, double y, double z) {
		renderOriginX = RenderCube.renderCubeOrigin(MathHelper.fastFloor(x));
		renderOriginY = RenderCube.renderCubeOrigin(MathHelper.fastFloor(y));
		renderOriginZ = RenderCube.renderCubeOrigin(MathHelper.fastFloor(z));
	}

	public void setAbsoluteRenderOrigin(double x, double y, double z) {
		renderOriginX = x;
		renderOriginY = y;
		renderOriginZ = z;
	}

	public final AbstractVertexCollector get(MaterialState materialState) {
		final int materialIndex = materialState.index;
		AbstractVertexCollector result = collectors[materialIndex];

		if(result == null) {
			result = emptyCollector().prepare(materialState);
			collectors[materialIndex] = result;
		}

		return result;
	}

	private AbstractVertexCollector emptyCollector() {
		AbstractVertexCollector result;

		if(usedCount == allCollectors.size()) {
			result = new AbstractVertexCollector(this);
			allCollectors.add(result);
		} else {
			result = allCollectors.get(usedCount);
		}

		usedCount++;
		return result;
	}

	public final void forEachExisting(Consumer<AbstractVertexCollector> consumer) {
		final int usedCount = this.usedCount;
		for(int i = 0; i < usedCount; i++) {
			consumer.accept(allCollectors.get(i));
		}
	}

	/**
	 * Sorts pipelines by pipeline index numerical order.
	 * DO NOT RETAIN A REFERENCE
	 */
	public final BufferPackingList packingListSolid() {
		final BufferPackingList packing = packingList;
		packing.clear();

		final int usedCount = this.usedCount;
		final Object[] collectors = allCollectors.elements();

		Arrays.sort(collectors, 0, usedCount, solidComparator);

		for (int i = 0; i < usedCount; i++) {
			final AbstractVertexCollector vertexCollector = (AbstractVertexCollector) collectors[i];
			final int vertexCount = vertexCollector.vertexCount();

			if (vertexCount != 0) {
				packing.addPacking(vertexCollector.materialState(), 0, vertexCount);
			}
		}

		return packing;
	}

	public final UploadableChunk.Solid packUploadSolid() {
		final BufferPackingList packing = packingListSolid();

		// NB: for solid render, relying on pipelines being added to packing in
		// numerical order so that
		// all chunks can iterate pipelines independently while maintaining same
		// pipeline order within chunk
		return packing.size() == 0 ? null : new UploadableChunk.Solid(packing, this);
	}

	/**
	 * Sorts pipelines from camera - more costly to produce and render.
	 * DO NOT RETAIN A REFERENCE
	 */
	public final BufferPackingList packingListTranslucent() {
		final BufferPackingList packing = packingList;
		packing.clear();
		final PriorityQueue<AbstractVertexCollector> sorter = sorters.get();

		final double x = viewX - renderOriginX;
		final double y = viewY - renderOriginY;
		final double z = viewZ - renderOriginZ;

		// Sort quads within each pipeline, while accumulating in priority queue
		final int usedCount = this.usedCount;

		for (int i = 0; i < usedCount; i++) {
			final AbstractVertexCollector vertexCollector = allCollectors.get(i);

			if (vertexCollector.vertexCount() != 0) {
				vertexCollector.sortQuads(x, y, z);
				sorter.add(vertexCollector);
			}
		}

		// exploit special case when only one transparent pipeline in this render chunk
		if (sorter.size() == 1) {
			final AbstractVertexCollector only = sorter.poll();
			packing.addPacking(only.materialState(), 0, only.vertexCount());
		} else if (sorter.size() != 0) {
			AbstractVertexCollector first = sorter.poll();
			AbstractVertexCollector second = sorter.poll();

			do {
				// x4 because packing is vertices vs quads
				final int startVertex = first.sortReadIndex() * 4;
				packing.addPacking(first.materialState(), startVertex, 4 * first.unpackUntilDistance(second.firstUnpackedDistance()));

				if (first.hasUnpackedSortedQuads()) {
					sorter.add(first);
				}

				first = second;
				second = sorter.poll();

			} while (second != null);

			final int startVertex = first.sortReadIndex() * 4;
			packing.addPacking(first.materialState(), startVertex, 4 * first.unpackUntilDistance(Double.MIN_VALUE));
		}
		return packing;
	}

	public final UploadableChunk.Translucent packUploadTranslucent() {
		final BufferPackingList packing = packingListTranslucent();
		return packing.size() == 0 ? null : new UploadableChunk.Translucent(packing, this);
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
			final AbstractVertexCollector vc = emptyCollector().loadState(data);
			collectors[vc.materialState().index] = vc;
		}
	}

	private static final Comparator<AbstractVertexCollector> translucentComparator = new Comparator<AbstractVertexCollector>() {
		@Override
		public int compare(AbstractVertexCollector o1, AbstractVertexCollector o2) {
			// note reverse order - take most distant first
			return Double.compare(o2.firstUnpackedDistance(), o1.firstUnpackedDistance());
		}
	};

	private static final Comparator<Object> solidComparator = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return Long.compare(((AbstractVertexCollector)o1).materialState().sortIndex, ((AbstractVertexCollector)o2).materialState().sortIndex);
		}
	};

	private static final ThreadLocal<PriorityQueue<AbstractVertexCollector>> sorters = new ThreadLocal<PriorityQueue<AbstractVertexCollector>>() {
		@Override
		protected PriorityQueue<AbstractVertexCollector> initialValue() {
			return new PriorityQueue<>(translucentComparator);
		}

		@Override
		public PriorityQueue<AbstractVertexCollector> get() {
			final PriorityQueue<AbstractVertexCollector> result = super.get();
			result.clear();
			return result;
		}
	};

	private static final AbstractVertexCollector[] EMPTY = new AbstractVertexCollector[MaterialState.MAX_MATERIAL_STATES];
}
