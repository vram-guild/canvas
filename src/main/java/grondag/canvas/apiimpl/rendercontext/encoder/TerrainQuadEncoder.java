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

package grondag.canvas.apiimpl.rendercontext.encoder;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;

import io.vram.frex.api.model.InputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.format.TerrainEncoder;
import grondag.canvas.buffer.format.TerrainEncodingContext;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.CanvasRenderMaterial;

public class TerrainQuadEncoder extends BaseQuadEncoder {
	// WIP: make this part of the encoder itself
	public final TerrainEncodingContext encodingContext = new TerrainEncodingContext() { };

	public TerrainQuadEncoder() {
		collectors = new VertexCollectorList(true, true);
	}

	@Override
	public void accept(BaseQuadEmitter quad, InputContext inputContext, @Nullable VertexConsumer defaultConsumer) {
		TerrainEncoder.encodeQuad(quad, inputContext, encodingContext, collectors.get((CanvasRenderMaterial) quad.material()));
	}
}
