/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.apiimpl.rendercontext.encoder;

import java.util.BitSet;

import org.jetbrains.annotations.Nullable;

import io.vram.frex.api.model.InputContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.mixinterface.SpriteExt;

public abstract class BaseQuadEncoder {
	protected final BaseQuadEmitter emitter;
	protected final InputContext inputContext;

	public BaseQuadEncoder(BaseQuadEmitter emitter, InputContext inputContext) {
		this.emitter = emitter;
		this.inputContext = inputContext;
	}

	public @Nullable VertexCollectorList collectors = null;

	public final BitSet animationBits = new BitSet();

	protected void trackAnimation(BaseQuadEmitter quad) {
		final var mat = quad.material();

		if (!mat.discardsTexture() && mat.texture().isAtlas()) {
			// WIP: create and use sprite method on quad
			final int animationIndex = ((SpriteExt) mat.texture().spriteIndex().fromIndex(quad.spriteId())).canvas_animationIndex();

			if (animationIndex >= 0) {
				animationBits.set(animationIndex);
			}
		}
	}
}
