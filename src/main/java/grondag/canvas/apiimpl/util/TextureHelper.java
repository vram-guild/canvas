/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.apiimpl.util;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;

import io.vram.frex.api.mesh.QuadEditor;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

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
		UVLOCKERS[Direction.EAST.getId()] = (q, i) -> q.spriteFloat(i, 1 - q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.WEST.getId()] = (q, i) -> q.spriteFloat(i, q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.NORTH.getId()] = (q, i) -> q.spriteFloat(i, 1 - q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.SOUTH.getId()] = (q, i) -> q.spriteFloat(i, q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.DOWN.getId()] = (q, i) -> q.spriteFloat(i, q.x(i), 1 - q.z(i));
		UVLOCKERS[Direction.UP.getId()] = (q, i) -> q.spriteFloat(i, q.x(i), q.z(i));
	}

	private TextureHelper() {
	}

	/**
	 * Bakes textures in the provided vertex data, handling UV locking,
	 * rotation, interpolation, etc. Textures must not be already baked.
	 */
	public static void bakeSprite(MutableQuadViewImpl quad, Sprite sprite, int bakeFlags) {
		quad.setSpriteNormalized();
		quad.spriteId(((SpriteExt) sprite).canvas_id());

		if (quad.nominalFace() != null && (QuadEditor.BAKE_LOCK_UV & bakeFlags) != 0) {
			// Assigns normalized UV coordinates based on vertex positions
			applyModifier(quad, UVLOCKERS[quad.nominalFace().getId()]);
		} else if ((QuadEditor.BAKE_NORMALIZED & bakeFlags) == 0) {
			// Scales from 0-16 to 0-1
			applyModifier(quad, (q, i) -> q.spritePrecise(i, q.spritePreciseU(i) >> 4, q.spritePreciseV(i) >> 4));
		}

		final int rotation = bakeFlags & 3;

		if (rotation != 0) {
			// Rotates texture around the center of sprite.
			// Assumes normalized coordinates.
			applyModifier(quad, ROTATIONS[rotation]);
		}

		if ((QuadEditor.BAKE_FLIP_U & bakeFlags) != 0) {
			// Inverts U coordinates.  Assumes normalized (0-1) values.
			applyModifier(quad, (q, i) -> q.spritePrecise(i, UV_PRECISE_UNIT_VALUE - q.spritePreciseU(i), q.spritePreciseV(i)));
		}

		if ((QuadEditor.BAKE_FLIP_V & bakeFlags) != 0) {
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
