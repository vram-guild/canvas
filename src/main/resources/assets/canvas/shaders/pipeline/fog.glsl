#include frex:shaders/api/context.glsl
#include canvas:shaders/internal/world.glsl

/******************************************************
  canvas:shaders/pipeline/fog.glsl
******************************************************/

uniform int _cvu_fog_mode;

#define _CV_FOG_MODE = 0;

#define  _CV_FOG_LINEAR  0
#define  _CV_FOG_EXP     1
#define  _CV_FOG_EXP2    2
#define  _CV_FOG_DISABLE 3

#define _CV_FOG_CONFIG_VANILLA    0
#define _CV_FOG_CONFIG_SUBTLE    1

#define _CV_FOG_CONFIG _CV_FOG_CONFIG_VANILLA

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float _cv_linearFogFactor() {
	float fogFactor = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
	return clamp(fogFactor, 0.0, 1.0);
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float _cv_expFogFactor() {
	float f = gl_FogFragCoord * gl_Fog.density;
	float fogFactor = _cvu_fog_mode == _CV_FOG_EXP ? exp(f) : exp(f * f);
	return clamp(1.0 / fogFactor, 0.0, 1.0);
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float _cv_fogFactor() {
	return _cvu_fog_mode == _CV_FOG_LINEAR ? _cv_linearFogFactor() : _cv_expFogFactor();
}

vec4 _cv_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_SUBTLE
	float f = 1.0 - _cv_fogFactor();
	f *= f;
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, 1.0 - f);
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, _cv_fogFactor());
#endif
}

vec4 _cv_fog(vec4 diffuseColor) {
	return _cvu_fog_mode == _CV_FOG_DISABLE ? diffuseColor : _cv_fogInner(diffuseColor);
}
