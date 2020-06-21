#include canvas:shaders/internal/world.glsl

/******************************************************
  canvas:shaders/api/world.glsl

  Utilities for querying world information.
******************************************************/

/*
 * The number of seconds this world has been rendering since the last render
 * reload, including fractional seconds.
 *
 * Use this for effects that need a smoothly increasing counter.
 */
float cv_renderSeconds() {
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
float cv_worldDay() {
	return _cvu_world[_CV_WORLD_DAYS];
}

/*
 * Time of the currently rendering world with values 0 to 1.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Use this for effects that depend on the time of day.
 * Received from server - may not be smoothly incremented.
 */
float cv_worldTime() {
	return _cvu_world[_CV_WORLD_TIME];
}

/*
 * Ambient light intensity of the currently rendering world.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Experimental, likely to change.
 */
float cv_ambientIntensity() {
	return _cvu_world[_CV_AMBIENT_INTENSITY];
}

/*
 * Gamma-corrected max light color from lightmap texture.
 * Updated whenever lightmap texture is updated.
 *
 * Multiply full-brightness outputs by this to be consistent
 * with the game's brightness settings.
 *
 * Note that Canvas normally handles this automatically.
 * It is exposed for exotic use cases.
 */
vec4 cv_emissiveColor() {
	return vec4(_cvu_world[_CV_EMISSIVE_COLOR_RED], _cvu_world[_CV_EMISSIVE_COLOR_GREEN], _cvu_world[_CV_EMISSIVE_COLOR_BLUE], 1.0);
}

/*
 * Size of the moon the currently rendering world. Values are 0 to 1.
 */
float cv_moonSize() {
	return _cvu_world[_CV_MOON_SIZE];
}

/*
 * True when the currently rendering world has a sky with a light source.
 */
bool cv_worldHasSkylight() {
	return bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_HAS_SKYLIGHT) == 1;
}

/*
 * True when the currently rendering world is the Overworld.
 */
bool cv_isWorldTheOverworld() {
	return bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_OVERWORLD) == 1;
}

/*
 * True when the currently rendering world is the Nether.
 */
bool cv_isWorldTheNether() {
	return bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_NETHER) == 1;
}

/*
 * True when the currently rendering world is the End.
 */
bool cv_isWorldTheEnd() {
	return bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_END) == 1;
}

