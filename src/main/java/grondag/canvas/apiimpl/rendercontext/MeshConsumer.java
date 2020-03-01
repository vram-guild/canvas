/*******************************************************************************
 * Copyright 2019, 2020 grondag
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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Consumer;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.mesh.MeshImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.material.MaterialState;

/**
 * Consumer for pre-baked meshes.  Works by copying the mesh data to a
 * "editor" quad held in the instance, where all transformations are applied before buffering.
 */
public class MeshConsumer implements Consumer<Mesh> {
	private final AbstractRenderContext context;

	protected MeshConsumer(AbstractRenderContext context) {
		this.context = context;
	}

	/**
	 * Where we handle all pre-buffer coloring, lighting, transformation, etc.
	 * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
	 */
	private class Maker extends MutableQuadViewImpl implements QuadEmitter {
		{
			data = new int[MeshEncodingHelper.TOTAL_QUAD_STRIDE];
			material(Canvas.MATERIAL_STANDARD);
		}

		// only used via RenderContext.getEmitter()
		@Override
		public Maker emit() {
			lightFace(GeometryHelper.lightFace(this));
			ColorHelper.applyDiffuseShading(this, false);
			renderQuad(this);
			clear();
			return this;
		}
	}

	private final Maker editorQuad = new Maker();

	@Override
	public void accept(Mesh mesh) {
		final MeshImpl m = (MeshImpl) mesh;
		final int[] data = m.data();
		final int limit = data.length;
		int index = 0;

		while (index < limit) {
			System.arraycopy(data, index, editorQuad.data(), 0, MeshEncodingHelper.TOTAL_QUAD_STRIDE);
			editorQuad.load();
			index += MeshEncodingHelper.TOTAL_QUAD_STRIDE;
			renderQuad(editorQuad);
		}
	}

	public QuadEmitter getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}

	private void renderQuad(MutableQuadViewImpl quad) {
		if (!context.transform(editorQuad)) {
			return;
		}

		if (!context.cullTest(quad.cullFace())) {
			return;
		}

		MaterialState.get(context.materialContext(), quad).encoder.encodeQuad(quad, context);
	}
}
