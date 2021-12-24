/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.material.state;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2IntFunction;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class CollectorIndexMap {
	public static final int MAX_COLLECTOR_COUNT = 4096;
	static int nextIndex = 0;

	private static final long[] KEYS_BY_INDEX = new long[MAX_COLLECTOR_COUNT];
	private static final RenderState[] RENDER_STATES = new RenderState[MAX_COLLECTOR_COUNT];
	static final Long2IntOpenHashMap MAP = new Long2IntOpenHashMap(256, Hash.VERY_FAST_LOAD_FACTOR);

	private static final Long2IntFunction FUNC = key -> {
		final int result = nextIndex++;
		RENDER_STATES[result] = RenderState.fromBits(key);
		KEYS_BY_INDEX[result] = key;
		return result;
	};

	public static synchronized int indexFromKey(long collectorKey) {
		return MAP.computeIfAbsent(collectorKey, FUNC);
	}

	public static long keyFromIndex(int index) {
		return KEYS_BY_INDEX[index];
	}

	public static RenderState renderStateForIndex(int index) {
		return RENDER_STATES[index];
	}
}
