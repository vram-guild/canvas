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

package grondag.canvas.terrain.util;

import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.BitStorage;

import grondag.canvas.mixinterface.BitStorageExt;

/**
 * Minimize new allocation for chunk storage copies by recycling the arrays.
 */
public class PackedIntegerStorageHelper {
	private static final ArrayBlockingQueue<IntArrayList> POOL = new ArrayBlockingQueue<>(1024);

	private static IntArrayList claimList() {
		final IntArrayList list = POOL.poll();
		return list == null ? new IntArrayList(512) : list;
	}

	public static void release(IntArrayList list) {
		list.clear();
		POOL.offer(list);
	}

	public static IntArrayList claim(BitStorage array) {
		final IntArrayList list = claimList();
		((BitStorageExt) array).canvas_fastForEach(list);
		return list;
	}
}
