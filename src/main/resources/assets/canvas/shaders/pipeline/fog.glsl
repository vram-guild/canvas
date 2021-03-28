#include frex:shaders/api/context.glsl
#include frex:shaders/api/fog.glsl
#include canvas:shaders/pipeline/options.glsl
#include frex:shaders/api/player.glsl
#include canvas:fog_config

/******************************************************
  canvas:shaders/pipeline/fog.glsl
******************************************************/

vec4 p_fogInner(vec4 diffuseColor) {
#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_NONE
	if (!frx_playerHasEffect(FRX_EFFECT_BLINDNESS)) {
		return diffuseColor;
	}
#endif

	float fogFactor = smoothstep(frxFogStart, frxFogEnd, frx_distance);

#if _CV_FOG_CONFIG == _CV_FOG_CONFIG_SUBTLE
	float f = 1.0 - fogFactor;
	f *= f;
	return vec4(mix(frxFogColor.rgb, diffuseColor.rgb, 1.0 - f), diffuseColor.a);
#else
	return vec4(mix(frxFogColor.rgb, diffuseColor.rgb, fogFactor * frxFogColor.a), diffuseColor.a);
#endif
}

vec4 p_fog(vec4 diffuseColor) {
	return frxFogEnabled ? p_fogInner(diffuseColor) : diffuseColor;
}
