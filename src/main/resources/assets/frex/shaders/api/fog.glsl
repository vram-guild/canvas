#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/flags.glsl

/******************************************************
  frex:shaders/api/fog.glsl
******************************************************/
#define frx_fogColor _cvu_world[_CV_FOG_COLOR]
#define frx_fogStart _cvu_world[_CV_RENDER_INFO].x
#define frx_fogEnd _cvu_world[_CV_RENDER_INFO].y
#define frx_fogEnabled _CV_GET_FLAG(_CV_FLAG_ENABLE_FOG)

// PRE-RELEASE SUPPORT - DO NOT USE
#define frx_fogStart() frx_fogStart
#define frx_fogEnd() frx_fogEnd
#define vec4 frx_fogColor() frx_fogColor
#define frx_fogEnabled() (frx_fogEnabled == 1)

#define frxFogColor _cvu_world[_CV_FOG_COLOR]
#define frxFogStart _cvu_world[_CV_RENDER_INFO].x
#define frxFogEnd _cvu_world[_CV_RENDER_INFO].y
#define frxFogEnabled _CV_GET_FLAG(_CV_FLAG_ENABLE_FOG)
