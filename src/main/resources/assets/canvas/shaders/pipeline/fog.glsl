#include frex:shaders/api/context.glsl
#include frex:shaders/api/fog.glsl
#include canvas:shaders/pipeline/options.glsl

/******************************************************
  canvas:shaders/pipeline/fog.glsl
******************************************************/

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float _cp_linearFogFactor() {
	float fogFactor = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
	return clamp(fogFactor, 0.0, 1.0);
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float _cp_expFogFactor() {
	float f = gl_FogFragCoord * gl_Fog.density;
	float fogFactor = frx_fogMode() == FOG_EXP ? exp(f) : exp(f * f);
	return clamp(1.0 / fogFactor, 0.0, 1.0);
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float _cp_fogFactor() {
	return frx_fogMode() == FOG_LINEAR ? _cp_linearFogFactor() : _cp_expFogFactor();
}

vec4 _cp_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_SUBTLE
	float f = 1.0 - _cp_fogFactor();
	f *= f;
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, 1.0 - f);
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, _cp_fogFactor());
#endif
}

vec4 _cp_fog(vec4 diffuseColor) {
	return frx_fogMode() == FOG_DISABLE ? diffuseColor : _cp_fogInner(diffuseColor);
}
