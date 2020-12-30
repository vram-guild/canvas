#include frex:shaders/api/context.glsl
#include canvas:shaders/internal/varying.glsl

/******************************************************
  frex:shaders/api/sampler.glsl
******************************************************/

uniform sampler2D frxs_spriteAltas;

/**
 * When a texture atlas is in use, the renderer will automatically
 * map from normalized coordinates to texture coordinates before the
 * fragment shader runs. But this doesn't help if you want to
 * re-sample during fragment shading using normalized coordinates.
 *
 * This function will remap normalized coordinates to atlas coordinates
 * during fragment shading and also during frx_endVertex().  It has no
 * effect when the bound texture is not an atlas texture.
 *
 * Note that normalized texture coordinates aren't normally saved to
 * a variable because they aren't usually needed. If that is needed,
 * material shaders should use one of the frx_var0-4 variables defined
 * in vertex.glsl to avoid redundant declarations from different materials.
 */
vec2 frx_mapNormalizedUV(vec2 coord) {
	return _cvv_spriteBounds.xy + coord * _cvv_spriteBounds.zw;
}

#ifdef VANILLA_LIGHTING
uniform sampler2D frxs_lightmap;
#endif
