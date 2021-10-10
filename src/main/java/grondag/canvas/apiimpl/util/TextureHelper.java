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

package grondag.canvas.apiimpl.util;

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import io.vram.frex.api.buffer.QuadEmitter;

import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.mixinterface.SpriteExt;

/**
 * Handles most texture-baking use cases for model loaders and model libraries
 * via {@link #bakeSprite(MutableQuadView, int, Sprite, int)}. Also used by the API
 * itself to implement automatic block-breaking models for enhanced models.
 */
public class TextureHelper {
	private static final VertexModifier[] ROTATIONS = new VertexModifier[]{null,
		(q, i) -> q.spritePrecise(i, q.spritePreciseV(i), q.spritePreciseU(i)), //90
		(q, i) -> q.spritePrecise(i, UV_PRECISE_UNIT_VALUE - q.spritePreciseU(i), UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i)), //180
		(q, i) -> q.spritePrecise(i, UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i), q.spritePreciseU(i)) // 270
	};
	private static final VertexModifier[] UVLOCKERS = new VertexModifier[6];

	static {
		UVLOCKERS[Direction.EAST.get3DDataValue()] = (q, i) -> q.spriteFloat(i, 1 - q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.WEST.get3DDataValue()] = (q, i) -> q.spriteFloat(i, q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.NORTH.get3DDataValue()] = (q, i) -> q.spriteFloat(i, 1 - q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.SOUTH.get3DDataValue()] = (q, i) -> q.spriteFloat(i, q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.DOWN.get3DDataValue()] = (q, i) -> q.spriteFloat(i, q.x(i), 1 - q.z(i));
		UVLOCKERS[Direction.UP.get3DDataValue()] = (q, i) -> q.spriteFloat(i, q.x(i), q.z(i));
	}

	private TextureHelper() {
	}

	/**
	 * Bakes textures in the provided vertex data, handling UV locking,
	 * rotation, interpolation, etc. Textures must not be already baked.
	 */
	public static void bakeSprite(QuadEditorImpl quad, TextureAtlasSprite sprite, int bakeFlags) {
		quad.setSpriteNormalized();
		quad.spriteId(((SpriteExt) sprite).canvas_id());

		if (quad.nominalFace() != null && (QuadEmitter.BAKE_LOCK_UV & bakeFlags) != 0) {
			// Assigns normalized UV coordinates based on vertex positions
			applyModifier(quad, UVLOCKERS[quad.nominalFace().get3DDataValue()]);
		} else if ((QuadEmitter.BAKE_NORMALIZED & bakeFlags) == 0) {
			// Scales from 0-16 to 0-1
			applyModifier(quad, (q, i) -> q.spritePrecise(i, q.spritePreciseU(i) >> 4, q.spritePreciseV(i) >> 4));
		}

		final int rotation = bakeFlags & 3;

		if (rotation != 0) {
			// Rotates texture around the center of sprite.
			// Assumes normalized coordinates.
			applyModifier(quad, ROTATIONS[rotation]);
		}

		if ((QuadEmitter.BAKE_FLIP_U & bakeFlags) != 0) {
			// Inverts U coordinates.  Assumes normalized (0-1) values.
			applyModifier(quad, (q, i) -> q.spritePrecise(i, UV_PRECISE_UNIT_VALUE - q.spritePreciseU(i), q.spritePreciseV(i)));
		}

		if ((QuadEmitter.BAKE_FLIP_V & bakeFlags) != 0) {
			// Inverts V coordinates.  Assumes normalized (0-1) values.
			applyModifier(quad, (q, i) -> q.spritePrecise(i, q.spritePreciseU(i), UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i)));
		}
	}

	private static void applyModifier(QuadEditorImpl quad, VertexModifier modifier) {
		for (int i = 0; i < 4; i++) {
			modifier.apply(quad, i);
		}
	}

	@FunctionalInterface
	private interface VertexModifier {
		void apply(QuadEditorImpl quad, int vertexIndex);
	}
}
