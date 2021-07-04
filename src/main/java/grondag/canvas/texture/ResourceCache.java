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

package grondag.canvas.texture;

import java.util.function.Supplier;

public final class ResourceCache<T> {
	public ResourceCache(Supplier<T> loader) {
		ResourceCacheManager.CACHED.add(this);
		this.loader = loader;
	}

	private final Supplier<T> loader;
	private T value;

	public void invalidate() {
		value = null;
	}

	public T getOrLoad() {
		if (value == null) {
			value = loader.get();
		}

		return value;
	}
}
