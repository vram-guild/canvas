/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.render.terrain.drawlist;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.AbstractGlBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.varia.GFX;

public class SlabIndex extends AbstractGlBuffer {
	private SlabIndex() {
		// NB: STATIC makes a huge positive difference on AMD at least
		super(SLAB_INDEX_BYTES, GFX.GL_ELEMENT_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
		assert RenderSystem.isOnRenderThread();

		final TransferBuffer transferBuffer = TransferBuffers.claim(SLAB_INDEX_BYTES);
		final ByteBuffer buff = transferBuffer.byteBuffer();
		int triVertexIndex = 0;
		int quadVertexIndex = 0;

		while (quadVertexIndex < MAX_SLAB_INDEX_QUAD_VERTEX_COUNT) {
			buff.putShort(triVertexIndex, (short) quadVertexIndex);
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 1));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 2));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 2));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) (quadVertexIndex + 3));
			triVertexIndex += 2;
			buff.putShort(triVertexIndex, (short) quadVertexIndex);
			triVertexIndex += 2;
			quadVertexIndex += 4;
		}

		GFX.bindBuffer(bindTarget, glBufferId());
		transferBuffer.transferToBoundBuffer(bindTarget, 0, 0, SLAB_INDEX_BYTES);
		GFX.bindBuffer(bindTarget, 0);
		transferBuffer.release();
	}

	/** Ideally large enough to handle an entire draw list but not so large to push it out of VRAM. */
	private static final int SLAB_INDEX_BYTES = 0x80000;

	/** Six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3. */
	public static final int INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER = 6 * 2 / 4;

	/** Largest multiple of four vertices that, when expanded to triangles, will fit within the index buffer. */
	public static final int MAX_SLAB_INDEX_QUAD_VERTEX_COUNT = (SLAB_INDEX_BYTES / INDEX_QUAD_VERTEX_TO_TRIANGLE_BYTES_MULTIPLIER) & 0xFFFFFFF8;

	private static SlabIndex instance;

	public static SlabIndex get() {
		SlabIndex result = instance;

		if (result == null) {
			result = new SlabIndex();
			instance = result;
		}

		return result;
	}

	@Override
	protected void onShutdown() {
		// NOOP
	}
}
