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

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;

public class CanvasOutlineImmediate implements MultiBufferSource {
	private final MultiBufferSource fallbackSource;
	private OutlineBufferSource outlineSource;

	public CanvasOutlineImmediate(MultiBufferSource fallbackSource) {
		this.fallbackSource = fallbackSource;
	}

	public void setOutlineSource(OutlineBufferSource outlineSource) {
		this.outlineSource = outlineSource;
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		if (renderType.isOutline()) {
			return outlineSource.getBuffer(renderType);
		} else {
			VertexConsumer fallbackConsumer = fallbackSource.getBuffer(renderType);
			Optional<RenderType> optional = renderType.outline();

			if (optional.isPresent()) {
				VertexConsumer outlineConsumer = outlineSource.getBuffer(optional.get());
				return VertexMultiConsumer.create(outlineConsumer, fallbackConsumer);
			}

			return fallbackConsumer;
		}
	}

	public void setColor(int teamColor) {
		final int red = (teamColor >> 16 & 255);
		final int green = (teamColor >> 8 & 255);
		final int blue = teamColor & 255;
		outlineSource.setColor(red, green, blue, 255);
	}
}
