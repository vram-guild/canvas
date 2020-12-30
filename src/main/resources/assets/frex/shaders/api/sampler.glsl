#include frex:shaders/api/context.glsl

/******************************************************
  frex:shaders/api/sampler.glsl
******************************************************/

uniform sampler2D frxs_spriteAltas;

#ifdef VANILLA_LIGHTING
uniform sampler2D frxs_lightmap;
#endif
