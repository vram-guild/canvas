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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Consumer;

import io.vram.frex.api.mesh.Mesh;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MeshImpl;
import grondag.canvas.apiimpl.mesh.QuadEditorImpl;

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
			material(Canvas.MATERIAL_STANDARD);
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
