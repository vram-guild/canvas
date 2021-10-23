package grondag.canvas.buffer.input;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.Mth;

import grondag.canvas.render.terrain.TerrainFormat;

class BucketSorter {
	private final IntArrayList[] buckets = new IntArrayList[7];
	private int[] swapData;

	BucketSorter() {
		for (int i = 0; i < 7; ++i) {
			buckets[i] = new IntArrayList();
		}
	}

	void clear() {
		for (final var b : buckets) {
			b.clear();
		}
	}

	VertexBucket[] sort(int[] vertexData, int integerSize) {
		int[] swapData = this.swapData;

		if (swapData == null || swapData.length < integerSize) {
			swapData = new int[Mth.smallestEncompassingPowerOfTwo(integerSize)];
			this.swapData = swapData;
		}

		System.arraycopy(vertexData, 0, swapData, 0, integerSize);
		final VertexBucket[] result = new VertexBucket[7];
		int baseVertexIndex = 0;
		int targetIndex = 0;

		for (int i = 0; i < 7; ++i) {
			final var bucket = buckets[i];

			for (final int sourceIndex : bucket) {
				System.arraycopy(swapData, sourceIndex, vertexData, targetIndex, TerrainFormat.TERRAIN_MATERIAL.quadStrideInts);
				targetIndex += TerrainFormat.TERRAIN_MATERIAL.quadStrideInts;
			}

			final int vertexCount = bucket.size() * 4;
			result[i] = new VertexBucket(baseVertexIndex, vertexCount);
			baseVertexIndex += vertexCount;

			assert baseVertexIndex * TerrainFormat.TERRAIN_MATERIAL.vertexStrideInts == targetIndex;
		}

		assert targetIndex == integerSize;

		return result;
	}

	void add(int bucketIndex, int integerIndex) {
		buckets[bucketIndex].add(integerIndex);
	}
}
