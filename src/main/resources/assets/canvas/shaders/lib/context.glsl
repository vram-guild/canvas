#include canvas:shaders/lib/constant.glsl

#define AO_MODE_NORMAL 0
#define AO_MODE_SUBTLE_ALWAYS 1
#define AO_MODE_SUBTLE_BLOCK_LIGHT 2
#define AO_MODE_NONE 3

// true if AO shading should be applied
#define AO_SHADING_MODE AO_MODE_NORMAL

#define DIFFUSE_MODE_NORMAL 0
#define DIFFUSE_MODE_SKY_ONLY 1
#define DIFFUSE_MODE_NONE 2

// true if diffuse shading should be applied
#define DIFFUSE_SHADING_MODE DIFFUSE_MODE_NORMAL

// true if lighting should be noised to prevent mach banding
// will only be enabled if smooth light is also enabled
#define ENABLE_LIGHT_NOISE FALSE

// true if this is a block context
#define CONTEXT_IS_BLOCK FALSE

// true if this is an item context
#define CONTEXT_IS_ITEM FALSE

// true if this is a GUI context
#define CONTEXT_IS_GUI FALSE

#define HARDCORE_DARKNESS FALSE

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec4 u_emissiveColor;
uniform vec3 u_eye_position;
