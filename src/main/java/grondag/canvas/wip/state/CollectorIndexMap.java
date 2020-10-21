/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.wip.state;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class CollectorIndexMap {
	public static final int MAX_COLLECTOR_COUNT = 4096;
	static int nextIndex = 0;

	private static final long[] KEYS_BY_INDEX = new long[MAX_COLLECTOR_COUNT];
	private static final WipRenderState[] RENDER_STATES = new WipRenderState[MAX_COLLECTOR_COUNT];
	static final Long2IntOpenHashMap MAP = new Long2IntOpenHashMap(256, Hash.VERY_FAST_LOAD_FACTOR);

	private static final Long2IntFunction FUNC = key -> {
		final int result = nextIndex++;
		RENDER_STATES[result] = WipRenderStateFinder.threadLocal().fromBits(key);
		KEYS_BY_INDEX[result] = key;
		return result;
	};

	public static synchronized int indexFromKey(long collectorKey) {
		return MAP.computeIfAbsent(collectorKey, FUNC);
	}

	public static long keyFromIndex(int index) {
		return KEYS_BY_INDEX[index];
	}

	public static WipRenderState renderStateForIndex(int index) {
		return RENDER_STATES[index];
	}
}
