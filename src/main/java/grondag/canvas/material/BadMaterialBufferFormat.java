package grondag.canvas.material;


import java.nio.ByteBuffer;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

/**
 * CPU-side vertex format - vertices with same format stride and draw handler can share the same draw call
 * Encoder and draw handler output/input formats must match.
 */
public enum BadMaterialBufferFormat {
	VANILLA_BLOCKS_AND_ITEMS(8);

	public final int vertexStrideBytes;

	public final int vertexStrideInts;

	// TODO: implement or remove
	public final int attributeCount = -1;

	private BadMaterialBufferFormat(int vertexStrideBytes) {
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
	public static BadMaterialBufferFormat get(MaterialContext context, Value mat, MutableQuadViewImpl quad) {
		return VANILLA_BLOCKS_AND_ITEMS;

	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VAO VBOs
	 */
	public void enableAndBindAttributes(int bufferOffset) {
		// TODO: implement or remove
	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VBO buffers.
	 */
	public void enableAndBindAttributes(ByteBuffer buffer, int bufferOffset) {
		// TODO: implement or remove
	}

	/**
	 * Binds attribute locations without enabling them. For use with VAOs. In other
	 * cases just call {@link #enableAndBindAttributes(int)}
	 * @param attribCount How many attributes are currently enabled.  Any not in format should be bound to dummy index.
	 */
	public void bindAttributeLocations(int bufferOffset, int attribCount) {
		// TODO: implement or remove
	}
}
