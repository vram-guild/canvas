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

import grondag.frex.api.material.RenderMaterial;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;


public final class WipRenderMaterial extends AbstractRenderState implements RenderMaterial {
	public final int collectorIndex;
	public final WipRenderState renderState;
	public final int shaderFlags;

	WipRenderMaterial(long bits) {
		super(nextIndex++, bits);
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey());
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderFlags = shaderFlags();
	}

	private static ThreadLocal<WipRenderMaterialFinder> FINDER = ThreadLocal.withInitial(WipRenderMaterialFinder::new);

	public static WipRenderMaterialFinder finder() {
		final WipRenderMaterialFinder result = FINDER.get();
		result.clear();
		return result;
	}

	static int nextIndex = 0;
	static final ObjectArrayList<WipRenderMaterial> LIST = new ObjectArrayList<>();
	static final Long2ObjectOpenHashMap<WipRenderMaterial> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	public static final WipRenderMaterial MISSING = new WipRenderMaterial(0);

	static {
		LIST.add(MISSING);
	}

	public static WipRenderMaterial fromIndex(int index) {
		return LIST.get(index);
	}
}
