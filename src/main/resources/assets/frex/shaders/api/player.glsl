#include canvas:shaders/internal/world.glsl

/******************************************************
  frex:shaders/api/player.glsl

  Utilities for querying player information.
******************************************************/

/*
 * Magnitude of effects that affect vision: night vision, being in fluid, etc.
 * Experimental, likely to change.
 */
float frx_effectModifier() {
    return _cvu_world[_CV_WORLD_EFFECT_MODIFIER];
}

/*
 *  Color and magnitude of light source held by player in either hand.
 *  RGB are the light color, alpha channel holds the 0-1 magnitude.
 *
 *  Magnitude 1 currently represents a source that can reach 15 blocks.
 *  This scale is subject to change.
 *
 *  If the player is not holding a light source, all values are zero.
 */
vec4 frx_heldLight() {
	return vec4(_cvu_world[_CV_HELD_LIGHT_RED], _cvu_world[_CV_HELD_LIGHT_GREEN], _cvu_world[_CV_HELD_LIGHT_BLUE],  _cvu_world[_CV_HELD_LIGHT_INTENSITY]);
}

/*
 * True when player has the night vision effect.
 */
bool frx_playerHasNightVision() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_NIGHT_VISTION_ACTIVE) == 1;
}

