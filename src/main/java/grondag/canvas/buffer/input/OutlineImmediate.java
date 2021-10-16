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

package grondag.canvas.buffer.input;

import java.util.Optional;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;

import grondag.canvas.mixinterface.OutlineBufferSourceExt;

public class OutlineImmediate implements MultiBufferSource {
	private final CanvasImmediate fallbackSource;
	private final BufferSource itemBuffer;
	private BufferSource entityBuffer;

	private int red, green, blue;
	private boolean itemState = false;

	public OutlineImmediate(CanvasImmediate fallbackImmediate) {
		fallbackSource = fallbackImmediate;
		itemBuffer = MultiBufferSource.immediate(new BufferBuilder(256));
		red = green = blue = 255;
	}

	public void setOutlineBufferSource(OutlineBufferSource outlineBufferSource) {
		/* NB: This reduces the amount of buffers but can create similar state issues that led to using separate item
		/* buffer in the first place. If this happens, should consider creating own buffers and leave vanilla's alone. */
		var ext = (OutlineBufferSourceExt) outlineBufferSource;
		entityBuffer = ext.canvas_getOutlineBufferSource();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (renderType.isOutline()) {
			return createOutlineConsumer(renderType);
		} else {
			VertexConsumer fallbackConsumer = fallbackSource.getBuffer(renderType);
			Optional<RenderType> optional = renderType.outline();

			if (optional.isPresent()) {
				VertexConsumer outlineConsumer = createOutlineConsumer(optional.get());
				return VertexMultiConsumer.create(outlineConsumer, fallbackConsumer);
			}

			return fallbackConsumer;
		}
	}

	private VertexConsumer createOutlineConsumer(RenderType renderType) {
		// Item entities are causing state issues in our method, so they need to be separated from other entities.
		final MultiBufferSource buffer = itemState ? itemBuffer : entityBuffer;

		if (entityBuffer == null) {
			throw new IllegalStateException("Drawing entity outline without setting an outline buffer source.");
		}

		return new OutlineBufferSource.EntityOutlineGenerator(buffer.getBuffer(renderType), red, green, blue, 255);
	}

	public void entityState(int teamColor, boolean isItem) {
		itemState = isItem;
		red = (teamColor >> 16 & 255);
		green = (teamColor >> 8 & 255);
		blue = teamColor & 255;
	}

	public void endOutlineBatch() {
		entityBuffer.endBatch();
		itemBuffer.endBatch();
	}
}
