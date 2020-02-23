/*******************************************************************************
 * Copyright 2019 grondag
 *
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

package grondag.canvas.chunk;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.draw.DelegateLists;
import grondag.canvas.draw.DrawableDelegate;
import grondag.canvas.material.MaterialState;
import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderManager;

/**
 * Plays same role as VertexBuffer in RenderChunk but implementation is much
 * different.
 * <p>
 *
 * For solid layer, each pipeline will be separately collected into
 * memory-mapped buffers specific to that pipeline so that during render we are
 * able to render multiple chunks per pipeline out of the same buffer.
 * <p>
 *
 * For translucent layer, all pipelines will be collected into the same buffer
 * because rendering order must be maintained.
 * <p>
 *
 * In both cases, it is possible for a pipeline's vertices to span two buffers
 * because our memory-mapped buffers are fixed size.
 * <p>
 *
 * The implementation handles the draw commands and vertex attribute state but
 * relies on caller to manage shaders, uniforms, transforms or any other GL
 * state.
 * <p>
 *
 *
 */
public abstract class DrawableChunk {
	protected boolean isCleared = false;

	private int quadCount = -1;

	protected ObjectArrayList<DrawableDelegate> delegates;

	public DrawableChunk(ObjectArrayList<DrawableDelegate> delegates) {
		this.delegates = delegates;
	}

	public int drawCount() {
		return delegates.size();
	}

	public int quadCount() {
		int result = quadCount;
		if(result == -1) {
			result = 0;
			final int limit = delegates.size();
			for(int i = 0; i < limit; i++) {
				final DrawableDelegate d = delegates.get(i);
				result += d.bufferDelegate().byteCount() / d.materialState().materialVertexFormat().vertexStrideBytes / 4;
			}
			quadCount = result;
		}
		return result;
	}

	/**
	 * Called when buffer content is no longer current and will not be rendered.
	 */
	public final void clear() {
		if (!isCleared) {
			isCleared = true;
			assert delegates != null;
			if (!delegates.isEmpty()) {
				final int limit = delegates.size();
				for (int i = 0; i < limit; i++) {
					delegates.get(i).release();
				}
				delegates.clear();
			}
			DelegateLists.releaseDelegateList(delegates);
			delegates = null;
		}
	}

	@Override
	protected void finalize() {
		clear();
	}

	public static class Solid extends DrawableChunk {
		public Solid(ObjectArrayList<DrawableDelegate> delegates) {
			super(delegates);
		}

		/**
		 * Prepares for iteration and handles any internal housekeeping. Called each
		 * frame from client thread before any call to {@link #renderSolidNext()}.
		 */
		public void prepareSolidRender(Consumer<ObjectArrayList<DrawableDelegate>> consumer) {
			if (isCleared) {
				return;
			}
			consumer.accept(delegates);
		}
	}

	public static class Translucent extends DrawableChunk {
		public Translucent(ObjectArrayList<DrawableDelegate> delegates) {
			super(delegates);
		}

		public void renderChunkTranslucent() {
			if (isCleared) {
				return;
			}

			final int limit = delegates.size();

			if (limit == 0) {
				return;
			}

			final Object[] draws = delegates.elements();

			final int frameIndex = ShaderManager.INSTANCE.frameIndex();

			// using conventional loop here to prevent iterator garbage in hot loop
			// profiling shows it matters
			for (int i = 0; i < limit; i++) {
				final DrawableDelegate b = (DrawableDelegate) draws[i];
				final MaterialState p = b.materialState();
				if(!p.condition.affectBlocks || p.condition.compute(frameIndex)) {
					p.activate(ShaderContext.BLOCK_TRANSLUCENT);
					b.bind();
					b.draw();
				}
			}
		}
	}
}
