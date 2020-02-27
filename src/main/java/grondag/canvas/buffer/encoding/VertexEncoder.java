package grondag.canvas.buffer.encoding;

import grondag.canvas.material.MaterialBufferFormat;

public interface VertexEncoder {

	MaterialBufferFormat outputFormat();

	int index();
}
