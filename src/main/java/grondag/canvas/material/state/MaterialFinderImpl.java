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

package grondag.canvas.material.state;

import io.vram.frex.api.material.MaterialFinder;

// PERF: implement proper decal layers in JMX, RenderBender and XB/XM to improve performance for multi-layer blocks
public class MaterialFinderImpl extends AbstractStateFinder<MaterialFinderImpl, RenderMaterialImpl> implements MaterialFinder {
	private String label = CANVAS_MATERIAL_NAME;

	@Override
	public MaterialFinderImpl clear() {
		label = CANVAS_MATERIAL_NAME;
		return super.clear();
	}

	@Override
	public MaterialFinderImpl label(String name) {
		label = name;
		return this;
	}

	@Override
	protected synchronized RenderMaterialImpl findInner() {
		RenderMaterialImpl result = RenderMaterialImpl.MAP.get(bits);

		if (result == null) {
			result = new RenderMaterialImpl(bits, label);
			RenderMaterialImpl.MAP.put(bits, result);
			RenderMaterialImpl.VALUES[result.index] = result;
		}

		return result;
	}

	public static final String CANVAS_MATERIAL_NAME = "<unnamed material>";
}
