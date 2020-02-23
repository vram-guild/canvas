package grondag.canvas.buffer.encoding;

import grondag.canvas.pipeline.BufferFormat;

public interface VertexEncoder {

	BufferFormat outputFormat();

	int index();
}
