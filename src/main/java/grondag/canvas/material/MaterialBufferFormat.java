package grondag.canvas.material;


import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

/**
 * CPU-side vertex format - vertices with same format stride and draw handler can share the same draw call
 * Encoder and draw handler output/input formats must match.
 */
public enum MaterialBufferFormat {
	VANILLA_BLOCKS_AND_ITEMS(8);

	public final int vertexStrideBytes;

	public final int vertexStrideInts;

	private MaterialBufferFormat(int vertexStrideBytes) {
		this.vertexStrideBytes = vertexStrideBytes;
		vertexStrideInts = vertexStrideBytes / 4;
	}

	/**
	 * Determine minimal buffer format able to handle all attributes of the given quad/material/context.
	 * May use properties of the quad to select compact output formats.
	 *
	 * In terrain rendering, does not know the other content of the chunk, and would be expensive to use
	 * that information, so trade off will be size vs. number of draw calls.
	 */
	public static MaterialBufferFormat get(MaterialContext context, Value mat, MutableQuadViewImpl quad) {
		return VANILLA_BLOCKS_AND_ITEMS;

	}
}
