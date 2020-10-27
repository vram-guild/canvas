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

package grondag.canvas.material.state;

import grondag.frex.api.material.RenderMaterial;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

// WIP2: find way to implement efficient decal pass again, esp in terrain
// WIP2: make sure can handle "dual" render layers and similar vanilla constructs.
public final class RenderMaterialImpl extends AbstractRenderState implements RenderMaterial {
	public final int collectorIndex;
	public final RenderState renderState;
	public final int shaderFlags;

	RenderMaterialImpl(long bits) {
		super(nextIndex++, bits);
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey());
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderFlags = shaderFlags();
	}

	private static ThreadLocal<MaterialFinderImpl> FINDER = ThreadLocal.withInitial(MaterialFinderImpl::new);

	public static MaterialFinderImpl finder() {
		final MaterialFinderImpl result = FINDER.get();
		result.clear();
		return result;
	}

	static int nextIndex = 0;
	static final ObjectArrayList<RenderMaterialImpl> LIST = new ObjectArrayList<>();
	static final Long2ObjectOpenHashMap<RenderMaterialImpl> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	public static final RenderMaterialImpl MISSING = new RenderMaterialImpl(0);

	static {
		LIST.add(MISSING);
	}

	public static RenderMaterialImpl fromIndex(int index) {
		return LIST.get(index);
	}
}
