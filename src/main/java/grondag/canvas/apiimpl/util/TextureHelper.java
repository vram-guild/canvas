package grondag.canvas.apiimpl.util;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.mixinterface.SpriteExt;

/**
 * Handles most texture-baking use cases for model loaders and model libraries
 * via {@link #bakeSprite(MutableQuadView, int, Sprite, int)}. Also used by the API
 * itself to implement automatic block-breaking models for enhanced models.
 */
public class TextureHelper {
	private TextureHelper() { }

	/**
	 * Bakes textures in the provided vertex data, handling UV locking,
	 * rotation, interpolation, etc. Textures must not be already baked.
	 */
	public static void bakeSprite(MutableQuadViewImpl quad, int spriteIndex, Sprite sprite, int bakeFlags) {
		quad.setSpriteUnmapped(spriteIndex, true);
		quad.spriteId(spriteIndex, ((SpriteExt) sprite).canvas_id());

		if (quad.nominalFace() != null && (MutableQuadView.BAKE_LOCK_UV & bakeFlags) != 0) {
			// Assigns normalized UV coordinates based on vertex positions
			applyModifier(quad, spriteIndex, UVLOCKERS[quad.nominalFace().getId()]);
		} else if ((MutableQuadView.BAKE_NORMALIZED & bakeFlags) == 0) {
			// Scales from 0-16 to 0-1
			applyModifier(quad, spriteIndex, (q, i, t) -> q.spritePrecise(i, t, q.spritePreciseU(i, t) >> 4, q.spritePreciseV(i, t) >> 4));
		}

		final int rotation = bakeFlags & 3;

		if (rotation != 0) {
			// Rotates texture around the center of sprite.
			// Assumes normalized coordinates.
			applyModifier(quad, spriteIndex, ROTATIONS[rotation]);
		}

		if ((MutableQuadView.BAKE_FLIP_U & bakeFlags) != 0) {
			// Inverts U coordinates.  Assumes normalized (0-1) values.
			applyModifier(quad, spriteIndex, (q, i, t) -> q.spritePrecise(i, t, UV_PRECISE_UNIT_VALUE - q.spritePreciseU(i, t), q.spritePreciseV(i, t)));
		}

		if ((MutableQuadView.BAKE_FLIP_V & bakeFlags) != 0) {
			// Inverts V coordinates.  Assumes normalized (0-1) values.
			applyModifier(quad, spriteIndex, (q, i, t) -> q.spritePrecise(i, t, q.spritePreciseU(i, t), UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i, t)));
		}
	}

	@FunctionalInterface
	private interface VertexModifier {
		void apply(MutableQuadViewImpl quad, int vertexIndex, int spriteIndex);
	}

	private static void applyModifier(MutableQuadViewImpl quad, int spriteIndex, VertexModifier modifier) {
		for (int i = 0; i < 4; i++) {
			modifier.apply(quad, i, spriteIndex);
		}
	}

	private static final VertexModifier[] ROTATIONS = new VertexModifier[] { null,
			(q, i, t) -> q.spritePrecise(i, t, q.spritePreciseV(i, t), q.spritePreciseU(i, t)), //90
			(q, i, t) -> q.spritePrecise(i, t, UV_PRECISE_UNIT_VALUE - q.spritePreciseU(i, t), UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i, t)), //180
			(q, i, t) -> q.spritePrecise(i, t, UV_PRECISE_UNIT_VALUE - q.spritePreciseV(i, t), q.spritePreciseU(i, t)) // 270
	};

	private static final VertexModifier[] UVLOCKERS = new VertexModifier[6];

	static {
		UVLOCKERS[Direction.EAST.getId()] = (q, i, t) -> q.spriteFloat(i, t, 1 - q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.WEST.getId()] = (q, i, t) -> q.spriteFloat(i, t, q.z(i), 1 - q.y(i));
		UVLOCKERS[Direction.NORTH.getId()] = (q, i, t) -> q.spriteFloat(i, t, 1 - q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.SOUTH.getId()] = (q, i, t) -> q.spriteFloat(i, t, q.x(i), 1 - q.y(i));
		UVLOCKERS[Direction.DOWN.getId()] = (q, i, t) -> q.spriteFloat(i, t, q.x(i), 1 - q.z(i));
		UVLOCKERS[Direction.UP.getId()] = (q, i, t) -> q.spriteFloat(i, t, q.x(i), 1 - q.z(i));
	}
}
