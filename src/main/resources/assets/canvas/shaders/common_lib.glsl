#version 120
#extension GL_EXT_gpu_shader4 : enable

#define TRUE 1
#define FALSE 0

// will be changed to sprite depth (1, 2 or 3) before compile
#define LAYER_COUNT 1

// will be true all layers are cutout layers and fragment discard is possible
#define CUTOUT FALSE

// true if first sprite color is all white and will not be sent
// false if first sprite is colorized or compact formats are disabled
#define WHITE_0 FALSE

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

// true if using smooth lightmaps
// currently only enabled in block contexts
#define ENABLE_SMOOTH_LIGHT TRUE

// true if lighting should be noised to prevent mach banding
// will only be enabled if smooth light is also enabled
#define ENABLE_LIGHT_NOISE TRUE

// true if this is a block context
#define CONTEXT_IS_BLOCK TRUE

// true if this is an item context
#define CONTEXT_IS_ITEM TRUE

// true if this is a GUI context
#define CONTEXT_IS_GUI FALSE

#define HARDCORE_DARKNESS FALSE
#define SUBTLE_FOG FALSE

#define WORLD_EFFECT_MODIFIER 0
#define WORLD_NIGHT_VISION 1
#define WORLD_EFFECTIVE_INTENSITY 2
#define WORLD_AMBIENT_INTENSITY 3
#define WORLD_HAS_SKYLIGHT 4
#define WOLRD_DIMENSION_ID 5
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

#if ENABLE_SMOOTH_LIGHT
    uniform sampler2D u_utility;
    varying vec2 v_hd_blocklight;
    varying vec2 v_hd_skylight;
    varying vec2 v_hd_ao;

    //TODO: make this depend on shader props
    #if ENABLE_LIGHT_NOISE
        uniform sampler2D u_dither;
    #endif
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
        varying float v_diffuse;
#endif

//TODO - disable when not used
varying vec4 v_color_0;
varying vec2 v_texcoord_0;

//TODO - disable when not used
varying vec2 v_lightcoord;

#define USE_FLAT_VARYING FALSE

#if USE_FLAT_VARYING
    // may be faster when available and
    // prevents problems on some NVidia cards/drives
    flat varying vec2 v_flags;
#else
    // flat no available on mesa drivers
    invariant varying vec2 v_flags;
#endif

#if LAYER_COUNT > 1
varying vec4 v_color_1;
varying vec2 v_texcoord_1;
#endif

#if LAYER_COUNT > 2
varying vec4 v_color_2;
varying vec2 v_texcoord_2;
#endif

#define PI    3.1415926535897932384626433832795
#define PI_2  1.57079632679489661923

// packed in first flag octet
#define FLAG_EMISSIVE_0         0
#define FLAG_EMISSIVE_1         1
#define FLAG_EMISSIVE_2         2
#define FLAG_PADDING            3
#define FLAG_DISABLE_DIFFUSE_0  4
#define FLAG_DISABLE_AO_0       5
#define FLAG_CUTOUT_0           6
#define FLAG_UNMIPPED_0         7

// packed in second flag octet
#define FLAG_DISABLE_DIFFUSE_1  0
#define FLAG_DISABLE_AO_1       1
#define FLAG_CUTOUT_1           2
#define FLAG_UNMIPPED_1         3
#define FLAG_DISABLE_DIFFUSE_2  4
#define FLAG_DISABLE_AO_2       5
#define FLAG_CUTOUT_2           6
#define FLAG_UNMIPPED_2         7

#define  FACE_DOWN  0
#define  FACE_UP    1
#define  FACE_NORTH 2
#define  FACE_SOUTH 3
#define  FACE_WEST  4
#define  FACE_EAST  5

#define  FOG_LINEAR 9729
#define  FOG_EXP    2048
#define  FOG_EXP2   2049

#define LIGHTMAP_ADDRESS_SIZE 32768
#define LIGHTMAP_PIXEL_SIZE 8.0 / 32768.0

const mat3[6] UV_MATRIX = mat3[6](
        mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
        mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0),
        mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0)
);

int face(vec3 normal) {
    vec3 a = abs(normal);
    float m = max(max(a.x, a.y), a.z);

    return a.x == m ? (normal.x > 0 ? FACE_EAST : FACE_WEST)
            : a.y == m ? (normal.y > 0 ? FACE_UP : FACE_DOWN)
                    : (normal.z > 0 ? FACE_SOUTH : FACE_NORTH);
}

vec2 uv(vec3 pos, vec3 normal) {
    mat3 m = UV_MATRIX[face(normal)];
    vec3 result = m * pos;
    return result.xy;
}

/**
 * Formula mimics vanilla lighting for plane-aligned quads and is vaguely
 * consistent with Phong lighting ambient + diffuse for others.
 */
float diffuseBaked(vec3 normal) {
    return 0.5 + clamp(abs(normal.x) * 0.1 + (normal.y > 0 ? 0.5 * normal.y : 0.0) + abs(normal.z) * 0.3, 0.0, 0.5);
}

/**
 * Offers results simular to vanilla in Gui, assumes a fixed transform.
 */
float diffuseGui(vec3 normal) {
    // Note that vanilla rendering normally sends item models with raw colors and
    // canvas sends colors unmodified, so we do not need to compensate for any pre-buffer shading
    float light = 0.4
            + 0.6 * clamp(dot(normal.xyz, vec3(-0.309, 0.927, -0.211)), 0.0, 1.0)
            + 0.6 * clamp(dot(normal.xyz, vec3(0.518, 0.634, 0.574)), 0.0, 1.0);

    return min(light, 1.0);
}

/**
 * Unrotated, non-gui lights.  But not transformed into eye space.
 * Not sure how I want to do that yet.
 */
float diffuseWorld(vec3 normal) {
    float light = 0.4
            + 0.6 * clamp(dot(normal.xyz, vec3(0.16169, 0.808452, -0.565916)), 0.0, 1.0)
            + 0.6 * clamp(dot(normal.xyz, vec3(-0.16169, 0.808452, 0.565916)), 0.0, 1.0);

    return min(light, 1.0);
}

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


// from somewhere on the Internet...
float random (vec2 st) {
    return fract(sin(dot(st.xy,
            vec2(12.9898,78.233)))*
            43758.5453123);
}

// Ken Perlin's improved smoothstep
float smootherstep(float edge0, float edge1, float x) {
    // Scale, and clamp x to 0..1 range
    x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    // Evaluate polynomial
    return x * x * x * (x * (x * 6 - 15) + 10);
}

// Based in part on 2D Noise by Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float tnoise (in vec2 st, float t) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Compute values for four corners
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    a =  0.5 + sin((0.5 + a) * t) * 0.5;
    b =  0.5 + sin((0.5 + b) * t) * 0.5;
    c =  0.5 + sin((0.5 + c) * t) * 0.5;
    d =  0.5 + sin((0.5 + d) * t) * 0.5;

    // Mix 4 corners
    return mix(a, b, f.x) +
            (c - a)* f.y * (1.0 - f.x) +
            (d - b) * f.x * f.y;
}

const float[8] BITWISE_DIVISORS = float[8](0.5, 0.25, 0.125, 0.0625, 0.03125, 0.015625, 0.0078125, 0.00390625);

/**
 * Returns the value (0-1) of the indexed bit (0-7)
 * within a float value that represents a single byte (0-255).
 *
 * GLSL 120 unfortunately lacks bitwise operations
 * so we need to emulate them unless the extension is active.
 */
float bitValue(float byteValue, int bitIndex) {
    return floor(fract(byteValue * BITWISE_DIVISORS[bitIndex]) * 2.0);
}

/** Converts RGB to grayscale */
float luminance(vec3 color) {
    return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}
