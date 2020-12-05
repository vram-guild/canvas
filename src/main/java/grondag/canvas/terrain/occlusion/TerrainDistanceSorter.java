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

package grondag.canvas.terrain.occlusion;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

public class TerrainDistanceSorter {
	static final Int2IntOpenHashMap RINGS = new Int2IntOpenHashMap();
	static final int RING_COUNT;
	static final int MAX_SQ_DIST;
	static final int[] RING_INDEX;

	static {
		// 34 too allow for padding
		for (int x = -34; x <= 34; ++x) {
			for (int z = -34; z <= 34; ++z) {
				// not clamped on Y because player can be above or below world
				for (int y = -34; y <= 34; ++y) {
					final int d = x * x + y * y + z * z;
					RINGS.addTo(d, 1);
				}
			}
		}

		final int[] keys = RINGS.keySet().toIntArray();
		Arrays.sort(keys);

		RING_COUNT = keys.length;
		MAX_SQ_DIST = keys[RING_COUNT -1];
		RING_INDEX = new int[MAX_SQ_DIST + 1];
		Arrays.fill(RING_INDEX, -1);

		int index = 0;

		for (final int k : keys) {
			RING_INDEX[k] = index++;
		}

		assert index == RING_COUNT;
	}

	@SuppressWarnings("unchecked")
	final SimpleUnorderedArrayList<BuiltRenderRegion>[] rings = new SimpleUnorderedArrayList[RING_COUNT];
	int ringIndex;
	int regionIndex;

	public TerrainDistanceSorter() {
		for (int i = 0; i < RING_COUNT; ++i) {
			rings[i] = new SimpleUnorderedArrayList<>();
		}

		ringIndex = 0;
		regionIndex = 0;
	}

	public void clear() {
		for (int i = 0; i < RING_COUNT; ++i) {
			rings[i].clear();
		}

		ringIndex = 0;
		regionIndex = 0;
	}

	public void add(BuiltRenderRegion region) {
		final int dist = region.squaredChunkDistance();

		if (dist >= 0 && dist <= MAX_SQ_DIST) {
			rings[RING_INDEX[dist]].add(region);
		}
	}

	@Nullable BuiltRenderRegion next() {
		while (ringIndex < RING_COUNT) {
			final SimpleUnorderedArrayList<BuiltRenderRegion> ring = rings[ringIndex];

			if (ring.isEmpty() || regionIndex >= ring.size()) {
				regionIndex = 0;
				++ringIndex;
			} else {
				return ring.get(regionIndex++);
			}
		}

		return null;
	}
}
