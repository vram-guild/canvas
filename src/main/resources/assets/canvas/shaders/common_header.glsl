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
#define SUBTLE_FOG FALSE

#define WORLD_EFFECT_MODIFIER 0
#define WORLD_NIGHT_VISION 1
#define WORLD_EFFECTIVE_INTENSITY 2
#define WORLD_AMBIENT_INTENSITY 3
#define WORLD_HAS_SKYLIGHT 4
#define WOLRD_RESERVED 5
#define WOLRD_MOON_SIZE 6

#define DIMENSION_OVERWORLD 1
#define DIMENSION_NETHER 0
#define DIMENSION_END 2

uniform float[8] u_world;
uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec4 u_emissiveColor;
uniform vec3 u_eye_position;
uniform int u_fogMode;

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

#define FLAG_EMISSIVE           0 // 1 for emissive material
#define FLAG_DISABLE_DIFFUSE    1 // 1 if diffuse shade should not be applied
#define FLAG_DISABLE_AO         2 // 1 if ao shade should not be applied
#define FLAG_CUTOUT             3 // 1 if cutout layer - will only be set in base, non-translucent materials
#define FLAG_UNMIPPED           4 // 1 if LOD disabled - only set in conjunction with cutout
#define FLAG_RESERVED_5         5
#define FLAG_RESERVED_6         6
#define FLAG_RESERVED_7         7


float diffuse (vec3 normal) {
#if CONTEXT_IS_GUI
    return diffuseGui(normal);
#elif CONTEXT_IS_ITEM
    return diffuseGui(normal);
//    return diffuseWorld(normal);
#else
    return diffuseBaked(normal);
#endif
}
