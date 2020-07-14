
#include canvas:shaders/api/context.glsl
#include canvas:shaders/lib/constant.glsl
#include canvas:shaders/internal/context.glsl

/******************************************************
  canvas:shaders/internal/common_varying.glsl
******************************************************/

#if CONTEXT_IS_BLOCK
    varying float _cvv_ao;
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
     varying float _cvv_diffuse;
#endif

varying vec4 _cvv_color;
varying vec2 _cvv_texcoord;
varying vec2 _cvv_lightcoord;
varying vec3 _cvv_normal;

#if USE_FLAT_VARYING
    // may be faster when available and
    // prevents problems on some NVidia cards/drives
    flat varying vec4 _cvv_material;
#else
    // flat no available on mesa drivers
    invariant varying vec4 _cvv_material;
#endif
