
#include canvas:shaders/api/context.glsl
#include canvas:shaders/lib/constant.glsl
#include canvas:shaders/internal/internal_context.glsl

/******************************************************
  canvas:shaders/internal/common_varying.glsl
******************************************************/

#if CONTEXT_IS_BLOCK
    varying float v_ao;
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
     varying float v_diffuse;
#endif

varying vec4 v_color;
varying vec2 v_texcoord;
varying vec2 v_lightcoord;
varying vec3 v_normal;
