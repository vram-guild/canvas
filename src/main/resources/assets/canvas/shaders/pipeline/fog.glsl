#include frex:shaders/api/context.glsl
#include frex:shaders/api/fog.glsl
#include canvas:shaders/pipeline/options.glsl
#include frex:shaders/api/player.glsl
#include canvas:fog_config

/******************************************************
  canvas:shaders/pipeline/fog.glsl
******************************************************/

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float p_linearFogFactor() {
	float fogFactor = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
	return clamp(fogFactor, 0.0, 1.0);
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float p_expFogFactor() {
	float f = gl_FogFragCoord * gl_Fog.density;
	float fogFactor = frx_fogMode() == FOG_EXP ? exp(f) : exp(f * f);
	return clamp(1.0 / fogFactor, 0.0, 1.0);
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float p_fogFactor() {
	return frx_fogMode() == FOG_LINEAR ? p_linearFogFactor() : p_expFogFactor();
}

vec4 p_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_NONE
	if (!frx_playerHasEffect(FRX_EFFECT_BLINDNESS)) {
		return diffuseColor;
	}
#endif

#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_SUBTLE
	float f = 1.0 - p_fogFactor();
	f *= f;
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, 1.0 - f);
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, p_fogFactor());
#endif
}

vec4 p_fog(vec4 diffuseColor) {
	return frx_fogMode() == FOG_DISABLE ? diffuseColor : p_fogInner(diffuseColor);
}
