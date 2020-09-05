package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Consumer;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.material.AbstractMeshMaterial;
import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.MeshImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

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
			data = new int[MeshEncodingHelper.MAX_QUAD_STRIDE];
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

	public final Maker editorQuad = new Maker();

	@Override
	public void accept(Mesh mesh) {
		final MeshImpl m = (MeshImpl) mesh;
		final int[] data = m.data();
		final int limit = data.length;
		int index = 0;
		final MutableQuadViewImpl quad = editorQuad;

		while (index < limit) {
			final int stride = MeshEncodingHelper.stride(AbstractMeshMaterial.byIndex(data[index]).spriteDepth());
			quad.copyAndload(data, index, stride);
			index += stride;
			context.renderQuad();
		}
	}

	public QuadEmitter getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}

}
