#include frex:shaders/api/fog.glsl
#include abstract:shaders/pipeline/options.glsl
#include frex:shaders/api/player.glsl
#include abstract:fog_config

/******************************************************
  abstract:shaders/pipeline/fog.glsl
******************************************************/

vec4 p_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_NONE
	if (frx_effectBlindness != 1) {
		return diffuseColor;
	}
#endif

	float fogFactor = 1.0 - smoothstep(frx_fogStart, frx_fogEnd, frx_distance);

#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_SUBTLE
	fogFactor *= fogFactor;
#endif

	return vec4(mix(frx_fogColor.rgb, diffuseColor.rgb, fogFactor * frx_fogColor.a), diffuseColor.a);
}

vec4 p_fog(vec4 diffuseColor) {
	return frx_fogEnabled == 1 ? p_fogInner(diffuseColor) : diffuseColor;
}
