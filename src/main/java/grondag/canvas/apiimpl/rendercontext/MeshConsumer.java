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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Consumer;

import io.vram.frex.api.mesh.Mesh;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MeshImpl;
import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.material.state.RenderMaterialImpl;

/**
 * Consumer for pre-baked meshes.  Works by copying the mesh data to a
 * "editor" quad held in the instance, where all transformations are applied before buffering.
 */
public class MeshConsumer implements Consumer<Mesh> {
	public final Maker editorQuad = new Maker();
	private final AbstractRenderContext context;

	protected MeshConsumer(AbstractRenderContext context) {
		this.context = context;
	}

	@Override
	public void accept(Mesh mesh) {
		final MeshImpl m = (MeshImpl) mesh;
		final int[] data = m.data();
		final int limit = data.length;
		int index = 0;
		final QuadEditorImpl quad = editorQuad;

		while (index < limit) {
			final int stride = MeshEncodingHelper.stride();
			quad.copyAndLoad(data, index, stride);
			index += stride;
			context.renderQuad();
		}
	}

	public QuadEditorImpl getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}

	/**
	 * Where we handle all pre-buffer coloring, lighting, transformation, etc.
	 * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
	 */
	private class Maker extends QuadEditorImpl {
		{
			data = new int[MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE];
			material(RenderMaterialImpl.STANDARD_MATERIAL);
		}

		// only used via RenderContext.getEmitter()
		@Override
		public Maker emit() {
			complete();
			context.renderQuad();
			clear();
			return this;
		}
	}
}
