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
#ifdef SPARSE_LIGHT_DATA
uniform sampler2D frxs_lightData;
#else
uniform sampler3D frxs_lightData;
#endif

#define frx_getLightFiltered(worldPos) frx_getLightFiltered(frxs_lightData, worldPos)
#define frx_getLightRaw(worldPos) frx_getLightRaw(frxs_lightData, worldPos)
#define frx_getLight(worldPos, fallback) frx_getLight(frxs_lightData, worldPos, fallback)
#endif
