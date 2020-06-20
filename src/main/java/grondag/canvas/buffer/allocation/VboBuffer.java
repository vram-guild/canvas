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

package grondag.canvas.buffer.allocation;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.canvas.varia.GLBufferStore;
import grondag.canvas.varia.VaoStore;
import net.minecraft.client.render.VertexFormatElement;

public class VboBuffer implements AutoCloseable {
	ByteBuffer uploadBuffer;
	private int glBufferId = -1;
	private boolean isClosed = false;

	public final MaterialVertexFormat format;

	private final VertexBinder vertexBinder;

	@FunctionalInterface
	private interface VertexBinder {
		void bind();
	}

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = VAO_NONE;

	private static final int VAO_NONE = -1;

	public VboBuffer(int bytes, MaterialVertexFormat format) {
		// TODO: get rid of BufferAllocator if it won't be faster
		uploadBuffer = MemoryUtil.memAlloc(bytes); //BufferUtils.createByteBuffer(bytes); //BufferAllocator.claim(bytes);
		this.format = format;
		vertexBinder = CanvasGlHelper.isVaoEnabled() ? this::bindVao : this::bindVbo;
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		final ByteBuffer uploadBuffer = this.uploadBuffer;

		if(uploadBuffer != null) {
			uploadBuffer.rewind();
			BindStateManager.bind(glBufferId());
			GL21.glBufferData(GL21.GL_ARRAY_BUFFER, uploadBuffer, GL21.GL_STATIC_DRAW);
			BindStateManager.unbind();
			MemoryUtil.memFree(uploadBuffer);
			//BufferAllocator.release(uploadBuffer);
			this.uploadBuffer = null;
		}
	}

	private int glBufferId() {
		int result = glBufferId;

		if(result == -1) {
			assert RenderSystem.isOnGameThread();
			result = GLBufferStore.claimBuffer();

			assert result > 0;

			glBufferId = result;
		}

		return result;
	}

	public void bind() {
		assert RenderSystem.isOnRenderThread();
		vertexBinder.bind();
	}

	private void bindVao() {
		final MaterialVertexFormat format = this.format;

		if (vaoBufferId == VAO_NONE) {
			// Important this happens BEFORE anything that could affect vertex state
			CanvasGlHelper.glBindVertexArray(0);

			BindStateManager.bind(glBufferId());

			vaoBufferId = VaoStore.claimVertexArray();
			CanvasGlHelper.glBindVertexArray(vaoBufferId);

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.enableClientState(%d)", GL11.GL_VERTEX_ARRAY));
			}

			GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);

			if (Configurator.logGlStateChanges) {
				CanvasMod.LOG.info(String.format("GlState: GlStateManager.vertexPointer(%d, %d, %d, %d)", 3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, 0));
			}

			GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, 0);

			CanvasGlHelper.enableAttributesVao(format.attributeCount);
			format.bindAttributeLocations(0);
		} else {
			CanvasGlHelper.glBindVertexArray(vaoBufferId);
		}
	}

	private void bindVbo() {
		final MaterialVertexFormat format = this.format;
		BindStateManager.bind(glBufferId());

		if (Configurator.logGlStateChanges) {
			CanvasMod.LOG.info(String.format("GlState: GlStateManager.vertexPointer(%d, %d, %d, %d)", 3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, 0));
		}

		GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
		GlStateManager.vertexPointer(3, VertexFormatElement.Format.FLOAT.getGlId(), format.vertexStrideBytes, 0);
		format.enableAndBindAttributes(0);
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() {
		if (RenderSystem.isOnRenderThread()) {
			onClose();
		} else {
			RenderSystem.recordRenderCall(this::onClose);
		}
	}

	private void onClose() {
		if (!isClosed) {
			isClosed = true;

			final int glBufferId = this.glBufferId;

			if(glBufferId != -1) {
				GLBufferStore.releaseBuffer(glBufferId);
				this.glBufferId = -1;
			}

			final ByteBuffer uploadBuffer = this.uploadBuffer;

			if(uploadBuffer != null) {
				MemoryUtil.memFree(uploadBuffer);
				//BufferUtils.BufferAllocator.release(uploadBuffer);
				this.uploadBuffer = null;
			}

			if (vaoBufferId > 0) {
				VaoStore.releaseVertexArray(vaoBufferId);
				vaoBufferId = VAO_NONE;
			}
		}
	}

	public static void unbind() {
		BindStateManager.unbind();
	}

	public IntBuffer intBuffer() {
		return uploadBuffer.asIntBuffer();
	}
}
