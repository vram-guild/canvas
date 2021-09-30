/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
