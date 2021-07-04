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
	return _cvu_world[_CV_WORLD_TIME].x;
}

/*
 * The number of frames this world has been rendering since the last render
 * reload.
 *
 * Use this for effects that need a discrete increasing counter.
 */
uint frx_renderFrames() {
    return _cvu_world_uint[_CV_RENDER_FRAMES];
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
	return _cvu_world[_CV_WORLD_TIME].z;
}

/*
 * Time of the currently rendering world with values 0 to 1.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Use this for effects that depend on the time of day.
 * Received from server - may not be smoothly incremented.
 */
float frx_worldTime() {
	return _cvu_world[_CV_WORLD_TIME].y;
}


/*
 * Size of the moon the currently rendering world. Values are 0 to 1.
 */
float frx_moonSize() {
	return _cvu_world[_CV_WORLD_TIME].w;
}

/*
 * Rotation of the sky dome as part of the day/night cycle.
 * Will not advance if doDaylightCycle game rule is turned off.
 *
 * In vanilla dimensions, zero represents the condition at
 * tick = 0 (just after sunrise) and rotation is around the Z axis.
 *
 * Will be zero at noon, (world time = 6000) but vanilla adjusts the progression
 * so that the sun is slightly over the horizon at world time = 0 and
 * also takes its time setting as well.
 */
float frx_skyAngleRadians() {
	return _cvu_world[_CV_SKYLIGHT_VECTOR].w;
}

/*
 * Normalized vector of the primary sky light (the sun or moon) used for
 * shadow mapping and useful for direct sky lighting. Points towards the skylight.
 * Will not advance if doDaylightCycle game rule is turned off.
 *
 * See notes on frx_skyAngleRadians() regarding asymmetry.
 */
vec3 frx_skyLightVector() {
	return _cvu_world[_CV_SKYLIGHT_VECTOR].xyz;
}

/*
 * Linear RGB color of the most prevalent sky light - the one that is
 * used for shadow map and intended for directional lighting.
 *
 * Does not vary by time of day or for atmospheric effects but can vary
 * based on season or other celestial variables for modded dimensions
 * or if configured for vanilla dimensions.
 *
 * Not adjusted for night vision or any player effect. Does not account
 * for underwater or any other occlusion.
 * Not adjusted for rain, thunder, cloud cover, or lightning.
 *
 * Use the FRX_WORLD_IS_MOONLIT flag to query if this is sun or moonlight.
 * Will be unit vector if the world has no skylight.
 */
vec3 frx_skyLightColor() {
	return _cvu_world[_CV_SKYLIGHT_COLOR].xyz;
}

/*
 * Measures intensity of direct sky light in lumens/square meter. (lux)
 * Does not vary by time of day or for atmospheric effects but can vary
 * based on season or other celestial variables for modded dimensions
 * or if configured for vanilla dimensions. Also varies by moon phase
 * for moonlight if that feature is enabled.
 *
 * Not adjusted for night vision or any player effect. Does not account
 * for underwater or any other occlusion.
 * Not adjusted for rain, thunder, cloud cover, or lightning.
 *
 * FREX pegs vanilla sunlight illuminace at 32,000 lux.  Value could be
 * different for modded dimensions or if configured via resource pack.
 *
 * Moonlight in Vanilla appears to be more intense than in the real world.
 * If configured to mimic vanilla, moonlight will be ~100lux but
 * can be very small or zero (or perhaps higher) in other configurations.
 *
 * Values assume an idealized, non-specific reference white light.
 * When multiplied by frx_skyLightColor() the effective luminance
 * will be somewhat less, but shaders are not expected to compensate
 * for this. Most celestial light sources are some flavor of white,
 * and stongly-colored lights can be handled by adjusting the illuminance
 * in the mod/pack configuration if needed.
 *
 * Effective illuminance will also be reduced if light is multiplied by
 * frx_skyLightAtmosphericColor() but that should not require any
 * compensation because it mimics the scattering of light. Pipelines
 * that strive for physical realism will not use that factor anyway.
 *
 * Use the FRX_WORLD_IS_MOONLIT flag to query if this is sun or moonlight.
 * Will be zero if the world has no skylight.
 */
float frx_skyLightIlluminance() {
	return _cvu_world[_CV_SKYLIGHT_COLOR].w;
}

/*
 * Linear RGB color modifier for the most prevalent sky light - the one
 * that is used for shadow map and intended for directional lighting.
 *
 * Also adjusted by time of day to account for atmospheric effects.
 * Pipelines that model atmospheric scattering will not want this.
 *
 * Not adjusted for weather, lightning, night vision or any player effect.
 * Does not account for underwater or any other occlusion.
 *
 * Use the FRX_WORLD_IS_MOONLIT flag to query if this is sun or moonlight.
 * Will be unit vector if the world has no skylight.
 */
vec3 frx_skyLightAtmosphericColor() {
	return _cvu_world[_CV_ATMOSPEHRIC_COLOR].xyz;
}

/*
 * Smoothing factor to help with the transition from sun to/from moon light.
 *
 * Ramps down to zero down as the moon set and then back up to
 * one as the sun rises. In future, will be configurable by dimension.
 *
 * Use the FRX_WORLD_IS_MOONLIT flag to query if this is sun or moonlight.
 * Will be 1.0 (and mean nothing) if the world has no skylight.
 */
float frx_skyLightTransitionFactor() {
	return _cvu_world[_CV_ATMOSPEHRIC_COLOR].w;
}

/*
 * Ambient light intensity of the currently rendering world.
 * Zero represents the morning / start of the day cycle in Minecraft.
 *
 * Experimental, likely to change.
 */
float frx_ambientIntensity() {
	return _cvu_world[_CV_AMBIENT_LIGHT].a;
}

/*
 * DEPRECATED - better options now available

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
	return vec4(_cvu_world[_CV_AMBIENT_LIGHT].rgb, 1.0);
}

/*
 * MC rain strength. Values 0 to 1.
 */
float frx_rainGradient() {
	return _cvu_world[_CV_CAMERA_VIEW].w;
}

/*
 * MC thunder strength. Values 0 to 1.
 */
float frx_thunderGradient() {
	return _cvu_world[_CV_EYE_POSITION].w;
}

/**
 * Same as frx_rainGradient but with exponential smoothing.
 * Speed is controlled in pipeline config.
 */
float frx_smoothedRainGradient() {
	return _cvu_world[_CV_ENTITY_VIEW].w;
}

/*
 * True when world.isThundering() is true for the currently rendering world.
 */
bool frx_isThundering() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_THUNDERING) == 1.0;
}

/*
 * True when world.getSkyProperties().isDarkened() is true for the currently rendering world.
 * True in Nether - indicates diffuse lighting bottom face is same as top, not as bright.
 */
bool frx_isSkyDarkened() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_SKY_DARKENED) == 1.0;
}

/**
 * The background clear color as computed by vanilla logic. Incorporates
 * many different factors.  For use by pipelines that may want to modify
 * and do their own clearing operations. Pipelines can disable the vanilla
 * clear pass in pipeline config.
 *
 *
 * Material shader authors note: this may not be the actual clear color used
 * by the in-effect pipeline. (It might not clear with a single color at all!)
 */
vec3 frx_vanillaClearColor() {
	return _cvu_world[_CV_CLEAR_COLOR].rgb;
}

bool frx_testCondition(int conditionIndex) {
	return _cv_testCondition(conditionIndex);
}

// Tokens accepted in frx_worldFlag
// True when the currently rendering world has a sky with a light source.
#define FRX_WORLD_HAS_SKYLIGHT 0
// True when the currently rendering world is the Overworld.
#define FRX_WORLD_IS_OVERWORLD 1
// True when the currently rendering world is the Nether.
#define FRX_WORLD_IS_NETHER 2
// True when the currently rendering world is the End.
#define FRX_WORLD_IS_END 3
// True when world.isRaining() is true for the currently rendering world.
#define FRX_WORLD_IS_RAINING 4
// Bet you can guess
#define FRX_WORLD_IS_THUNDERING 5
// The sky - it is dark
#define FRX_WORLD_IS_SKY_DARKENED 6
// The sky light modeled by frx_skyLightStrength and frx_skyLightVector is the moon
#define FRX_WORLD_IS_MOONLIT 21

bool frx_worldFlag(int flag) {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], flag) == 1.0;
}

/*
 * DEPRECATED - use frx_worldFlag
 */
bool frx_worldHasSkylight() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_HAS_SKYLIGHT) == 1.0;
}

/*
 * DEPRECATED - use frx_worldFlag
 */
bool frx_isWorldTheOverworld() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_OVERWORLD) == 1.0;
}

/*
 * DEPRECATED - use frx_worldFlag
 */
bool frx_isWorldTheNether() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_NETHER) == 1.0;
}

/*
 * DEPRECATED - use frx_worldFlag
 */
bool frx_isWorldTheEnd() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_END) == 1.0;
}

/*
 * DEPRECATED - use frx_worldFlag
 */
bool frx_isRaining() {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], _CV_FLAG_IS_RAINING) == 1.0;
}

