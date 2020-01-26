/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.chunk;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Minimize new allocation for chunk storage copies by recycling the long arrays.
 */
public class PackedIntegerStorageHelper {

	private static final ArrayBlockingQueue<long[]> POOL_256 = new ArrayBlockingQueue<>(1024);
	private static final ArrayBlockingQueue<long[]> POOL_320 = new ArrayBlockingQueue<>(1024);

	private static long[] claim256() {
		final long[] result = POOL_256.poll();
		return result == null ? new long[256] : result;
	}

	private static long[] claim320() {
		final long[] result = POOL_320.poll();
		return result == null ? new long[320] : result;
	}

	public static void releaseStorageCopy(long[] storage) {
		final int len = storage.length;

		if(len == 256) {
			POOL_256.offer(storage);
		} else if(len == 320) {
			POOL_320.offer(storage);
		}
	}

	public static long[] claimStorageCopy(long[] storage) {
		final int len = storage.length;
		final long[] result;
		if(len == 256) {
			result = claim256();
		} else if(len == 320) {
			result = claim320();
		} else {
			result = new long[len];
		}
		System.arraycopy(storage, 0, result, 0, len);
		return result;
	}

}
