
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

#define USE_FLAT_VARYING FALSE

#if USE_FLAT_VARYING
    // may be faster when available and
    // prevents problems on some NVidia cards/drives
    flat varying float v_flags;
#else
    // flat no available on mesa drivers
    invariant varying float v_flags;
#endif
