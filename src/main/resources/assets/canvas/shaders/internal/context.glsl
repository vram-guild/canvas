/******************************************************
  canvas:shaders/internal/context.glsl
******************************************************/

#define AO_MODE_NORMAL 0
#define AO_MODE_SUBTLE_ALWAYS 1
#define AO_MODE_SUBTLE_BLOCK_LIGHT 2
#define AO_MODE_NONE 3

#define AO_SHADING_MODE AO_MODE_NORMAL

#define DIFFUSE_MODE_NORMAL 0
#define DIFFUSE_MODE_SKY_ONLY 1
#define DIFFUSE_MODE_NONE 2

#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_NORMAL

// define if lighting should be noised to prevent mach banding
// will only be enabled if smooth light is also enabled
//#define ENABLE_LIGHT_NOISE

//#define USE_FLAT_VARYING

#define TARGET_BASECOLOR 0
#define TARGET_EMISSIVE -1

#define HANDHELD_LIGHT_RADIUS 0
