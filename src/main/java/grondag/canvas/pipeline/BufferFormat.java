package grondag.canvas.pipeline;

import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl.Value;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

/**
 * CPU-side vertex format - vertices with same format stride and draw handler can share the same draw call
 * Encoder and draw handler output/input formats must match.
 */
public interface BufferFormat {
	/**
	 * Determine minimal buffer format able to handle all attributes of the given quad/material/context.
	 * May use properties of the quad to select compact output formats.
	 *
	 * In terrain rendering, does not know the other content of the chunk, and would be expensive to use
	 * that information, so trade off will be size vs. number of draw calls.
	 *
	 * @param target
	 * @param subject
	 * @param mat
	 * @param quad
	 * @return
	 */
	static BufferFormat get(PipelineContext context, Value mat, MutableQuadViewImpl quad) {
		// TODO Auto-generated method stub
		return null;
	}

}
