#include frex:shaders/api/view.glsl
#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/flags.glsl

/******************************************************
  frex:shaders/api/world.glsl

  Utilities for querying world information.

  Some transformation-related methods have been moved
  to view.glsl, which is currently included to prevent
  breaks until shaders can be updated.
******************************************************/

/*
 * The number of seconds this world has been rendering since the last render
 * reload, including fractional seconds.
 *
 * Use this for effects that need a smoothly increasing counter.
 */
float frx_renderSeconds() {
	return _cvu_world[_CV_RENDER_SECONDS];
}

/*
 * Day of the currently rendering world - integer portion only.
 * This is the apparent day, not the elapsed play time, which can
 * be different due to sleeping, /set time, etc.
 *
 * Use this for effects that depend somehow on the season or age of the world.
 * Received from server - may not be smoothly incremented.
 */
float frx_worldDay() {
	return _cvu_world[_CV_WORLD_DAYS];
}

/*
 * Time of the currently rendering world with values 0 to 1.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Use this for effects that depend on the time of day.
 * Received from server - may not be smoothly incremented.
 */
float frx_worldTime() {
	return _cvu_world[_CV_WORLD_TIME];
}

/*
 * Ambient light intensity of the currently rendering world.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Experimental, likely to change.
 */
float frx_ambientIntensity() {
	return _cvu_world[_CV_AMBIENT_INTENSITY];
}

/*
 * Gamma-corrected max light color from lightmap texture.
 * Updated whenever lightmap texture is updated.
 *
 * Multiply emissive outputs by this to be consistent
 * with the game's brightness settings.
 *
 * Note that Canvas normally handles this automatically.
 * It is exposed for exotic use cases.
 */
vec4 frx_emissiveColor() {
	return vec4(_cvu_world[_CV_EMISSIVE_COLOR_RED], _cvu_world[_CV_EMISSIVE_COLOR_GREEN], _cvu_world[_CV_EMISSIVE_COLOR_BLUE], 1.0);
}

/*
 * Size of the moon the currently rendering world. Values are 0 to 1.
 */
float frx_moonSize() {
	return _cvu_world[_CV_MOON_SIZE];
}

/*
 * True when the currently rendering world has a sky with a light source.
 */
bool frx_worldHasSkylight() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_HAS_SKYLIGHT) == 1.0;
}

/*
 * True when the currently rendering world is the Overworld.
 */
bool frx_isWorldTheOverworld() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_OVERWORLD) == 1.0;
}

/*
 * True when the currently rendering world is the Nether.
 */
bool frx_isWorldTheNether() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_NETHER) == 1.0;
}

/*
 * True when the currently rendering world is the End.
 */
bool frx_isWorldTheEnd() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_END) == 1.0;
}

/*
 * True when world.isRaining() is true for the currently rendering world.
 */
bool frx_isRaining() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_RAINING) == 1.0;
}

/*
 * MC rain gradient. Values 0 to 1.
 */
float frx_rainGradient() {
	return _cvu_world[_CV_RAIN_GRADIENT];
}

/*
 * True when world.isThundering() is true for the currently rendering world.
 */
bool frx_isThundering() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_THUNDERING) == 1.0;
}

/*
 * True when world.getSkyProperties().isDarkened() is true for the currently rendering world.
 * True in Nether - indicates diffuse lighting bottom face is same as top, not as bright.
 */
bool frx_isSkyDarkened() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_START], _CV_FLAG0_IS_SKY_DARKENED) == 1.0;
}

bool frx_testCondition(int conditionIndex) {
	return _cv_testCondition(conditionIndex);
}
