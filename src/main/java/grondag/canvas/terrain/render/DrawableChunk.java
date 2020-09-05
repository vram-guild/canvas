package grondag.canvas.terrain.render;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.shader.ShaderPass;

public abstract class DrawableChunk {
	protected boolean isClosed = false;
	public final VboBuffer vboBuffer;

	protected DrawableChunk(VboBuffer vboBuffer) {
		this.vboBuffer = vboBuffer;
	}

	abstract public ObjectArrayList<DrawableDelegate> delegates(ShaderPass pass);

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	public final void close() {
		if (!isClosed) {
			isClosed = true;

			closeInner();

			vboBuffer.close();
		}
	}

	public final boolean isClosed() {
		return isClosed;
	}

	abstract protected void closeInner();

	private static class Solid extends DrawableChunk {
		private ObjectArrayList<DrawableDelegate> solid;
		private ObjectArrayList<DrawableDelegate> decal;

		public Solid(VertexCollectorList collectorList, VboBuffer vboBuffer) {
			super(vboBuffer);

			final IntBuffer intBuffer = vboBuffer.intBuffer();
			intBuffer.position(0);

			final int limit = collectorList.solidCount();
			int position = 0;

			final ObjectArrayList<DrawableDelegate> solid = DelegateLists.getReadyDelegateList();

			// solid pass
			for (int i = 0; i < limit; ++i) {
				final VertexCollectorImpl collector = collectorList.getSolid(i);

				if (collector.materialState().shaderPass == ShaderPass.SOLID) {
					final int vertexCount = collector.vertexCount();
					collector.toBuffer(intBuffer);
					solid.add(DrawableDelegate.claim(collector.materialState(), position, vertexCount));
					position += vertexCount;
				}
			}

			final ObjectArrayList<DrawableDelegate> decal;

			if (solid.isEmpty()) {
				this.solid = null;
				decal = solid;
			} else {
				this.solid = solid;
				decal = DelegateLists.getReadyDelegateList();
			}

			// decal pass
			for (int i = 0; i < limit; ++i) {
				final VertexCollectorImpl collector = collectorList.getSolid(i);

				if (collector.materialState().shaderPass == ShaderPass.DECAL) {
					final int vertexCount = collector.vertexCount();
					collector.toBuffer(intBuffer);
					decal.add(DrawableDelegate.claim(collector.materialState(), position, vertexCount));
					position += vertexCount;
				}
			}

			if (decal.isEmpty()) {
				this.decal = null;
				DelegateLists.releaseDelegateList(decal);
			} else {
				this.decal = decal;
			}
		}

		@Override
		public ObjectArrayList<DrawableDelegate> delegates(ShaderPass pass) {
			if (pass == ShaderPass.SOLID) {
				return solid;
			} else {
				assert pass == ShaderPass.DECAL;
				return decal;
			}
		}

		@Override
		protected void closeInner() {
			assert solid != null || decal != null;

			if (solid != null) {
				clearDelegateList(solid);
				solid = null;
			}

			if (decal != null) {
				clearDelegateList(decal);
				decal = null;
			}
		}
	}

	private static class Translucent extends DrawableChunk {
		private ObjectArrayList<DrawableDelegate> delegates;

		public Translucent(VertexCollectorList collectorList, VboBuffer vboBuffer) {
			super(vboBuffer);

			final IntBuffer intBuffer = vboBuffer.intBuffer();
			intBuffer.position(0);

			final VertexCollectorImpl collector = collectorList.getTranslucent();
			collector.toBuffer(intBuffer);

			final ObjectArrayList<DrawableDelegate> delegates = DelegateLists.getReadyDelegateList();
			delegates.add(DrawableDelegate.claim(collector.materialState(), 0, collector.vertexCount()));
			this.delegates = delegates;
		}

		@Override
		public ObjectArrayList<DrawableDelegate> delegates(ShaderPass pass) {
			assert pass == ShaderPass.TRANSLUCENT;
			return delegates;
		}

		@Override
		protected void closeInner() {
			assert delegates != null;
			clearDelegateList(delegates);
			delegates = null;
		}
	}

	private static class Dummy extends DrawableChunk {
		private final ObjectArrayList<DrawableDelegate> nothing = new ObjectArrayList<>();
		protected Dummy() {
			super(null);
			isClosed = true;
		}

		@Override
		public ObjectArrayList<DrawableDelegate> delegates(ShaderPass pass) {
			return nothing;
		}

		@Override
		protected void closeInner() {
			// NOOP
		}
	}

	public static DrawableChunk EMPTY_DRAWABLE = new DrawableChunk.Dummy();

	private static void clearDelegateList(ObjectArrayList<DrawableDelegate> delegates) {
		if (!delegates.isEmpty()) {
			final int limit = delegates.size();

			for (int i = 0; i < limit; i++) {
				delegates.get(i).release();
			}

			delegates.clear();
		}

		DelegateLists.releaseDelegateList(delegates);
	}

	public static DrawableChunk pack(VertexCollectorList collectorList, VboBuffer vboBuffer, boolean translucent) {
		return translucent ? new Translucent(collectorList, vboBuffer) : new Solid(collectorList, vboBuffer);
	}

}