package grondag.canvas.terrain.render;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.MaterialVertexFormat;

public class UploadableChunk {
	protected final VboBuffer vboBuffer;
	protected final DrawableChunk drawable;

	public UploadableChunk(VertexCollectorList collectorList, MaterialVertexFormat format, boolean translucent, int bytes) {
		vboBuffer = new VboBuffer(bytes, format);
		drawable = DrawableChunk.pack(collectorList, vboBuffer, translucent);
	}

	private UploadableChunk() {
		vboBuffer = null;
		drawable = DrawableChunk.EMPTY_DRAWABLE;
	}

	/**
	 * Will be called from client thread - is where flush/unmap needs to happen.
	 */
	public DrawableChunk produceDrawable() {
		vboBuffer.upload();
		return drawable;
	}

	public static final UploadableChunk EMPTY_UPLOADABLE = new UploadableChunk() {
		@Override
		public DrawableChunk produceDrawable() {
			return DrawableChunk.EMPTY_DRAWABLE;
		}
	};
}
