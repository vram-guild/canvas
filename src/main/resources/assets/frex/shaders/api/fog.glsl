#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/flags.glsl

/******************************************************
  frex:shaders/api/fog.glsl
******************************************************/

/**
 * True if current material should have fog.
 */
bool frx_fogEnabled() {
	return frx_bitValue(uint(_cvv_flags), _CV_FLAG_ENABLE_FOG) == 1.0;
}

float frx_fogStart() {
	return _cvu_fog_info.x;
}

float frx_fogEnd() {
	return _cvu_fog_info.y;
}

vec4 frx_fogColor() {
	return _cvu_world[_CV_FOG_COLOR];
}


// vec4
#define frxFogColor _cvu_world[_CV_FOG_COLOR]

// float
#define frxFogStart _cvu_fog_info.x

// float
#define frxFogEnd _cvu_fog_info.y

// bool
#define frxFogEnabled (_cvu_fog_info.x >= 0.0)
