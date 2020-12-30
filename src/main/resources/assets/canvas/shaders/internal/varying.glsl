#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/varying.glsl
******************************************************/

varying vec4 _cvv_color;
varying vec3 _cvv_normal;
varying vec3 _cvv_worldcoord;

#ifdef VANILLA_LIGHTING
varying vec2 _cvv_lightcoord;
varying float _cvv_ao;
#endif
