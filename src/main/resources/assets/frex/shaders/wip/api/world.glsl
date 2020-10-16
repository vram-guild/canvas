#include canvas:shaders/wip/world.glsl
#include canvas:shaders/wip/flags.glsl

/******************************************************
  frex:shaders/api/world.glsl

  Utilities for querying world information.
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
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_HAS_SKYLIGHT) == 1.0;
}

/*
 * True when the currently rendering world is the Overworld.
 */
bool frx_isWorldTheOverworld() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_OVERWORLD) == 1.0;
}

/*
 * True when the currently rendering world is the Nether.
 */
bool frx_isWorldTheNether() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_NETHER) == 1.0;
}

/*
 * True when the currently rendering world is the End.
 */
bool frx_isWorldTheEnd() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_END) == 1.0;
}

/*
 * True when world.isRaining() is true for the currently rendering world.
 */
bool frx_isRaining() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_RAINING) == 1.0;
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
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_THUNDERING) == 1.0;
}

/*
 * True when world.getSkyProperties().isDarkened() is true for the currently rendering world.
 * True in Nether - indicates diffuse lighting bottom face is same as top, not as bright.
 */
bool frx_isSkyDarkened() {
	return frx_bitValue(_cvu_world[_CV_FLAGS_0], _CV_FLAG0_IS_SKY_DARKENED) == 1.0;
}

/*
 * World coordinates for model space origin in the current invocation.
 * Add this to vertex position to get world position.
 */
vec3 frx_modelOriginWorldPos() {
	return _cvu_model_origin;
}

/*
 * Vertex coordinates are relative to the camera and include model transformations
 * as well as camera rotation and translation via MatrixStack.
 * The GL view matrix will be the identity matrix. (the default state in world render)
 * Used for most per-frame renders (entities, block entities, etc.)
 */
#define MODEL_ORIGIN_ENTITY 0

/*
 * Vertex coordinates are relative to the camera and include model translation, scaling
 * and billboard rotation plus camera translation via matrix stack but not camera rotation.
 * The GL view matrix will include camera rotation.
 * Used for particle rendering.
 */
#define MODEL_ORIGIN_PARTICLE 1

/*
 * Vertex coordinate are raw model coordinates.
 * Will need a view matrix update per draw.
 * Currently not used.
 */
#define MODEL_ORIGIN_MODEL 2

/*
 * Vertex coordinates are relative to a world region and
 * include all model transformations.
 * GL view matrix must be updated for both camera rotation and offset.
 * Used in terrain rendering. Canvas regions may be 16x16 or 256x256.
 */
#define MODEL_ORIGIN_REGION 3

/*
 * Vertex coordinates are relative to the screen.
 * Intended for GUI rendering.
 * Currently not used.
 */
#define MODEL_ORIGIN_SCREEN 4

/**
 * Describes how vertex coordinates relate to world and camera geometry.
 * Will be one of the MODEL_ORIGIN_ constants defined above.
 *
 * Except as noted, GL state is always assumed to have the projection
 * matrix set and view matrix set to identity. Some types have additional
 * view matrix transformation.
 */
int frx_modelOriginType() {
	return _cvu_model_origin_type;
}
