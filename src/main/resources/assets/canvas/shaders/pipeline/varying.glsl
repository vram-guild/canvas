#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/pipeline/varying.glsl
******************************************************/

varying vec4 pv_color;
varying vec3 pv_normal;

#ifdef VANILLA_LIGHTING
varying vec2 pv_lightcoord;
varying float pv_ao;
#endif
