package grondag.canvas.pipeline;

import grondag.canvas.draw.DrawHandler;

public class PipelineConfiguration {
	// vertices with the same target can share the same buffer
	public final PipelineTarget target;
	public final PipelineSubject subject;

	// controls how vertices are written to buffers and does the writing
	private VertexEncoder encoder;

	// cpu-side buffer format - vertices with same format stride and draw handler can share the same draw call.
	private BufferFormat format;

	// sets up gl state, updates uniforms and does draw.  For shaders, handles vertex attributes and will be the same shader.
	private DrawHandler drawHandler;

	private PipelineConfiguration(PipelineTarget target, PipelineSubject subject) {
		this.target = target;
		this.subject = subject;
	}
}
