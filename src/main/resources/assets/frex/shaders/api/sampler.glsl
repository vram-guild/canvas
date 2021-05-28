#include frex:shaders/api/context.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  frex:shaders/api/sampler.glsl
******************************************************/

uniform sampler2D frxs_baseColor;

/**
 * When a texture atlas is in use, the renderer will automatically
 * map from normalized coordinates to texture coordinates before the
 * fragment shader runs. But this doesn't help if you want to
 * re-sample during fragment shading using normalized coordinates.
 *
 * This function will remap normalized coordinates to atlas coordinates.
 * It has no effect when the bound texture is not an atlas texture.
 */
vec2 frx_mapNormalizedUV(vec2 coord) {
	return _cvv_spriteBounds.xy + coord * _cvv_spriteBounds.zw;
}

/**
 * Takes texture atlas coordinates and remaps them to normalized.
 * Has no effect when the bound texture is not an atlas texture.
 */
vec2 frx_normalizeMappedUV(vec2 coord) {
	return _cvv_spriteBounds.z == 0.0 ? coord : (coord - _cvv_spriteBounds.xy) / _cvv_spriteBounds.zw;
}

#ifdef VANILLA_LIGHTING
uniform sampler2D frxs_lightmap;
#endif


#ifdef SHADOW_MAP_PRESENT
#ifdef FRAGMENT_SHADER
// These sample the same underlying image array but have different sampler types.

// The shadow sampler type is useful for final map testing
// and exploits hardware accumulation of shadow test results.
uniform sampler2DArrayShadow frxs_shadowMap;

// The regular sampler type is useful for
// probing depth at specific points for PCSS or Contact-Hardening Shadows.
uniform sampler2DArray frxs_shadowMapTexture;
#endif
#endif
