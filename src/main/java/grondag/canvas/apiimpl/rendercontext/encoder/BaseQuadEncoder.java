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

import java.util.BitSet;

import org.jetbrains.annotations.Nullable;

import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.mixinterface.SpriteExt;

public abstract class BaseQuadEncoder implements QuadEncoder {
	/** null when not in world render loop/thread or when default consumer should be honored. */
	@Nullable public VertexCollectorList collectors = null;

	public final BitSet animationBits = new BitSet();

	protected abstract void encodeQuad(BaseQuadEmitter quad);

	@Override
	public void accept(BaseQuadEmitter quad) {
		final var mat = quad.material();

		if (!mat.discardsTexture() && mat.texture().isAtlas()) {
			// WIP: create and use sprite method on quad
			final int animationIndex = ((SpriteExt) mat.texture().spriteIndex().fromIndex(quad.spriteId())).canvas_animationIndex();

			if (animationIndex >= 0) {
				animationBits.set(animationIndex);
			}
		}

		encodeQuad(quad);
	}
}
