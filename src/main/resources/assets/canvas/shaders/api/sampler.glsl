#include canvas:shaders/api/context.glsl
/******************************************************
  canvas:shaders/api/sampler.glsl
******************************************************/

uniform sampler2D cvs_spriteAltas;
uniform sampler2D cvs_overlay;
uniform sampler2D cvs_lightmap;
uniform sampler1D cvs_spriteInfo;

#if VANILLA_LIGHTING != TRUE
	uniform sampler2D cvs_dither;
	uniform sampler2D cvs_hdLightmap;
#endif
