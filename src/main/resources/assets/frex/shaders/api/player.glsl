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
	return vec4(_cvu_world[_CV_HELD_LIGHT_RED], _cvu_world[_CV_HELD_LIGHT_GREEN], _cvu_world[_CV_HELD_LIGHT_BLUE], _cvu_world[_CV_HELD_LIGHT_INTENSITY]);
}


// Tokens accepted in frx_playerHasEffect
// Includes all vanilla player effects in 1.16.4
#define FRX_EFFECT_SPEED 0
#define FRX_EFFECT_SLOWNESS 1
#define FRX_EFFECT_HASTE 2
#define FRX_EFFECT_MINING_FATIGUE 3
#define FRX_EFFECT_STRENGTH 4
#define FRX_EFFECT_INSTANT_HEALTH 5
#define FRX_EFFECT_INSTANT_DAMAGE 6
#define FRX_EFFECT_JUMP_BOOST 7
#define FRX_EFFECT_NAUSEA 8
#define FRX_EFFECT_REGENERATION 9
#define FRX_EFFECT_RESISTANCE 10
#define FRX_EFFECT_FIRE_RESISTANCE 11
#define FRX_EFFECT_WATER_BREATHING 12
#define FRX_EFFECT_INVISIBILITY 13
#define FRX_EFFECT_BLINDNESS 14
#define FRX_EFFECT_NIGHT_VISION 15
#define FRX_EFFECT_HUNGER 16
#define FRX_EFFECT_WEAKNESS 17
#define FRX_EFFECT_POISON 18
#define FRX_EFFECT_WITHER 19
#define FRX_EFFECT_HEALTH_BOOST 20
#define FRX_EFFECT_ABSORPTION 21
#define FRX_EFFECT_SATURATION 22
#define FRX_EFFECT_GLOWING 23
#define FRX_EFFECT_LEVITATION 24
#define FRX_EFFECT_LUCK 25
#define FRX_EFFECT_UNLUCK 26
#define FRX_EFFECT_SLOW_FALLING 27
#define FRX_EFFECT_CONDUIT_POWER 28
#define FRX_EFFECT_DOLPHINS_GRACE 29
#define FRX_EFFECT_BAD_OMEN 30
#define FRX_EFFECT_HERO_OF_THE_VILLAGE 31

/*
 * Accepts one of the tokens defined above.  Note that different implementations
 * could define different numeric token values - always use the preprocessor token.
 */
bool frx_playerHasEffect(int effect) {
	return frx_bitValue(_cvu_flags[_CV_PLAYER_FLAGS_INDEX], effect) == 1;
}

/*
 * True when player has the night vision effect.
 *
 * DEPRECATED - use frx_playerHasEffect()
 */
bool frx_playerHasNightVision() {
	return frx_bitValue(_cvu_flags[_CV_PLAYER_FLAGS_INDEX], FRX_EFFECT_NIGHT_VISION) == 1;
}
