#include canvas:shaders/internal/program.glsl
#include frex:shaders/api/light.glsl

/****************************************************************
 * frex:shaders/api/sampler.glsl - Canvas Implementation
 ***************************************************************/

uniform sampler2D frxs_baseColor;

vec2 frx_mapNormalizedUV(vec2 coord) {
	return _cvv_spriteBounds.xy + coord * _cvv_spriteBounds.zw;
}

vec2 frx_normalizeMappedUV(vec2 coord) {
	return _cvv_spriteBounds.z == 0.0 ? coord : (coord - _cvv_spriteBounds.xy) / _cvv_spriteBounds.zw;
}

#ifdef VANILLA_LIGHTING
uniform sampler2D frxs_lightmap;
#endif

#ifdef SHADOW_MAP_PRESENT
#ifdef FRAGMENT_SHADER
uniform sampler2DArrayShadow frxs_shadowMap;
uniform sampler2DArray frxs_shadowMapTexture;
#endif
#endif

#ifdef COLORED_LIGHTS_ENABLED
uniform sampler2D frxs_lightData;

vec4 frx_getLightFiltered(vec3 worldPos) {
	return frx_getLightFiltered(frxs_lightData, worldPos);
}

vec4 frx_getLightRaw(vec3 worldPos) {
	return frx_getLightRaw(frxs_lightData, worldPos);
}

vec3 frx_getLight(vec3 worldPos, vec3 fallback) {
	return frx_getLight(frxs_lightData, worldPos, fallback);
}
#endif
