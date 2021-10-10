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

package grondag.canvas.apiimpl.mesh;

import io.vram.frex.api.mesh.Mesh;
import io.vram.frex.api.mesh.MeshBuilder;
import io.vram.frex.api.mesh.QuadEmitter;

/**
 * Our implementation of {@link MeshBuilder}, used for static mesh creation and baking.
 * Not much to it - mainly it just needs to grow the int[] array as quads are appended
 * and maintain/provide a properly-configured {@link net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView} instance.
 * All the encoding and other work is handled in the quad base classes.
 * The one interesting bit is in {@link Maker#emit()}.
 */
public class MeshBuilderImpl implements MeshBuilder {
	private final Maker maker = new Maker();
	int[] data = new int[256];
	int index = 0;
	int limit = data.length;

	protected void ensureCapacity(int stride) {
		if (stride > limit - index) {
			limit *= 2;
			final int[] bigger = new int[limit];
			System.arraycopy(data, 0, bigger, 0, index);
			data = bigger;
			maker.data = bigger;
		}
	}

	@Override
	public Mesh build() {
		final int[] packed = new int[index];
		System.arraycopy(data, 0, packed, 0, index);
		index = 0;
		maker.begin(data, index);
		return new MeshImpl(packed);
	}

	@Override
	public QuadEmitter getEmitter() {
		ensureCapacity(MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
		maker.begin(data, index);
		return maker;
	}

	/**
	 * Our base classes are used differently so we define final
	 * encoding steps in subtypes. This will be a static mesh used
	 * at render time so we want to capture all geometry now and
	 * apply non-location-dependent lighting.
	 */
	private class Maker extends QuadEditorImpl {
		@Override
		public Maker emit() {
			complete();
			index += MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE;
			ensureCapacity(MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
			baseIndex = index;
			clear();
			return this;
		}
	}
}
