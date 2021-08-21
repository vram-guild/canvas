#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/flags.glsl

/******************************************************
  frex:shaders/api/fog.glsl
******************************************************/
#define frx_fogColor _cvu_world[_CV_FOG_COLOR]
#define frx_fogStart _cvu_world[_CV_RENDER_INFO].x
#define frx_fogEnd _cvu_world[_CV_RENDER_INFO].y
#define frx_fogEnabled _CV_GET_FLAG(_CV_FLAG_ENABLE_FOG)
