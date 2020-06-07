
#include canvas:shaders/lib/context.glsl
#include canvas:shaders/lib/constant.glsl

#if CONTEXT_IS_BLOCK
    varying float v_ao;
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
     varying float v_diffuse;
#endif

//TODO - disable when not used
varying vec4 v_color;
varying vec2 v_texcoord;
varying vec2 v_lightcoord;
