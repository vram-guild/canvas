/******************************************************
  canvas:shaders/pipeline/options.glsl
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

#define HANDHELD_LIGHT_RADIUS 0

#define _CV_FOG_CONFIG_VANILLA    0
#define _CV_FOG_CONFIG_SUBTLE    1

#define _CV_FOG_CONFIG _CV_FOG_CONFIG_VANILLA

// define if lighting should be noised to prevent mach banding
// will only be enabled if smooth light is also enabled
//#define ENABLE_LIGHT_NOISE

#define TARGET_BASECOLOR 0
#define TARGET_EMISSIVE -1

// These won't go here if they are re-enabled
//#ifndef VANILLA_LIGHTING
//uniform sampler2D frxs_dither;
//uniform sampler2D frxs_hdLightmap;
//#endif
