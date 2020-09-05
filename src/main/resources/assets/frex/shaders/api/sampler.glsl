#include frex:shaders/api/context.glsl

/******************************************************
  frex:shaders/api/sampler.glsl
******************************************************/

uniform sampler2D frxs_spriteAltas;
uniform sampler2D frxs_overlay;
uniform sampler2D frxs_lightmap;
uniform sampler2D frxs_spriteInfo;

#ifndef VANILLA_LIGHTING
uniform sampler2D frxs_dither;
uniform sampler2D frxs_hdLightmap;
#endif
