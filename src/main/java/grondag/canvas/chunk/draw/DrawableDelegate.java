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

package grondag.canvas.chunk.draw;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.render.VertexFormatElement;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.allocation.AbstractBuffer;
import grondag.canvas.buffer.allocation.BufferDelegate;
import grondag.canvas.material.MaterialState;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.VaoStore;

public class DrawableDelegate {
	private static final ArrayBlockingQueue<DrawableDelegate> store = new ArrayBlockingQueue<>(4096);

	/**
	 * Signals the gl buffer not determined.
	 * Can't be populated at construction because that can happen off thread.
	 */
	private static final int BUFFER_UNKNOWN = -1000;

	/**
	 * Pointer to start of vertex data for current vertex binding.
	 * Set to zero when new vertex bindings applied.
	 * When vertex binding is updated, if buffer and format are the same and
	 * byte offset can be expressed as a multiple of current stride, then this
	 * is updated to a vertex offset to avoid rebinding vertex attributes.
	 */
	private static int vertexOffset = 0;

	/**
	 * Byte offset used for last vertex binding.
	 */
	private static int boundByteOffset = 0;

	/**
	 * Last format bound
	 */
	private static MaterialVertexFormat lastFormat = null;

	@FunctionalInterface
	private interface VertexBinder {
		void bind(MaterialVertexFormat format, boolean isNewBuffer);
	}

	public static DrawableDelegate claim(BufferDelegate bufferDelegate, MaterialState renderState, int vertexCount, MaterialVertexFormat format) {
		DrawableDelegate result = store.poll();

		if (result == null) {
			result = new DrawableDelegate();
		}

		result.bufferDelegate = bufferDelegate;
		result.materialState = renderState;
		result.vertexCount = vertexCount;
		result.isReleased = false;
		result.vertexBinder = bufferDelegate.buffer().isVbo()
				? (CanvasGlHelper.isVaoEnabled() ? result::bindVao : result::bindVbo)
						: result::bindBuffer;
				bufferDelegate.buffer().retain(result);
				result.bufferId = BUFFER_UNKNOWN;
				result.format = format;

				return result;
	}

	private BufferDelegate bufferDelegate;
	private MaterialState materialState;
	private int vertexCount;
	private boolean isReleased = false;
	private VertexBinder vertexBinder;
	private int bufferId = BUFFER_UNKNOWN;

	/** With translucency chunks may be different (padded) vs what material state would normally dictate - avoids attribute re-binding when VAO not an option */
	private MaterialVertexFormat format = null;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = -1;

	private DrawableDelegate() {
		super();
	}

	public BufferDelegate bufferDelegate() {
		return bufferDelegate;
	}

	/**
	 * Instances that share the same GL buffer will have the same ID. Allows sorting
	 * in solid layer to avoid rebinding buffers for draws that will have the same
	 * vertex buffer and pipeline/format.
	 */
	public int bufferId() {
		final int result = bufferId;
		if(bufferId == BUFFER_UNKNOWN) {
			bufferDelegate.buffer().bindable().glBufferId();
			bufferId = result;
		}
		return result;
	}

	/**
	 * The pipeline (and vertex format) associated with this delegate.
	 */
	public MaterialState materialState() {
		return materialState;
	}

	/**
	 * Won't bind buffer if this buffer same as last - will only do vertex
	 * attributes. Returns the buffer Id that is bound, or input if unchanged.
	 */
	public void bind() {
		final AbstractBuffer buffer = bufferDelegate.buffer();
		if (buffer.isDisposed()) {
			return;
		}

		// TODO: bind format here again
		@SuppressWarnings("unused")
		final boolean isNewBuffer = buffer.bindable().bind();
		//vertexBinder.bind(format, isNewBuffer);
	}

	// TODO: remove when vanilla hacks are done
	public int byteOffset() {
		return bufferDelegate.byteOffset();
	}

	/**
	 * Assumes pipeline has already been activated and buffer has already been bound
	 * via {@link #bind()}
	 */
	public void draw() {
		assert !isReleased;

		if (bufferDelegate.buffer().isDisposed()) {
			return;
		}

		GlStateManager.drawArrays(GL11.GL_QUADS, vertexOffset, vertexCount);
	}

	public void release() {
		if (!isReleased) {
			isReleased = true;
			bufferDelegate.buffer().release(this);
			bufferDelegate.release();
			bufferDelegate = null;

			if (vaoBufferId != -1) {
				VaoStore.releaseVertexArray(vaoBufferId);
				vaoBufferId = -1;
			}

			materialState =  null;
			store.offer(this);
		}
	}

	public void flush() {
		assert !isReleased;
		bufferDelegate.buffer().upload();
	}

	void bindVao(MaterialVertexFormat format, boolean isNewBuffer) {
		if (vaoBufferId == -1) {
			vaoBufferId = VaoStore.claimVertexArray();
			CanvasGlHelper.glBindVertexArray(vaoBufferId);

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.enableClientState(%d)", GL11.GL_VERTEX_ARRAY));
			}

			GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
			CanvasGlHelper.enableAttributesVao(format.attributeCount);

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.vertexPointer(%d, %d, %d, %d)", 3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, bufferDelegate.byteOffset()));
			}

			GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, bufferDelegate.byteOffset());
			format.bindAttributeLocations(bufferDelegate.byteOffset(), format.attributeCount);
		} else {
			CanvasGlHelper.glBindVertexArray(vaoBufferId);
		}
	}

	void bindVbo(MaterialVertexFormat format, boolean isNewBuffer) {
		// don't check for bind reuse if not possible due to new buffer
		final int byteOffset = bufferDelegate.byteOffset();

		if (isNewBuffer || format != lastFormat) {
			lastFormat = format;
			vertexOffset = 0;
			boundByteOffset = byteOffset;

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.vertexPointer(%d, %d, %d, %d)", 3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, byteOffset));
			}

			GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, byteOffset);
			format.enableAndBindAttributes(boundByteOffset);
		} else {
			// reuse vertex binding with offset
			final int gap = byteOffset - boundByteOffset;
			vertexOffset = gap / format.vertexStrideBytes;
		}
	}

	void bindBuffer(MaterialVertexFormat format, boolean isNewBuffer) {
		final int byteOffset = bufferDelegate.byteOffset();

		if (isNewBuffer || format != lastFormat) {
			final ByteBuffer buffer = bufferDelegate.buffer().byteBuffer();
			lastFormat = format;
			vertexOffset = 0;
			boundByteOffset = byteOffset;
			buffer.position(byteOffset);

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.enableClientState(%d)", GL11.GL_VERTEX_ARRAY));
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.vertexPointer(%d, %d, %d, %s)", 3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, buffer.toString()));
			}

			GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
			GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, MemoryUtil.memAddress(buffer));
			format.enableAndBindAttributes(buffer, byteOffset);
		} else {
			// reuse vertex binding with offset
			final int gap = byteOffset - boundByteOffset;
			vertexOffset = gap / format.vertexStrideBytes;
		}
	}
}
