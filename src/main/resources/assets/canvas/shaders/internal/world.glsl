#include frex:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/world.glsl
******************************************************/

// render seconds
// world days
// world time
// moon size
#define _CV_WORLD_TIME 0

// emissive rgb + ambient intensity
#define _CV_AMBIENT_LIGHT 1

// w is view distance
#define _CV_CLEAR_COLOR 2

// rgb + intensity
#define _CV_HELD_LIGHT_RGBI 3

// w is player mood
#define _CV_CAMERA_POS 4

// w is effect strength
#define _CV_LAST_CAMERA_POS 5

// w is darkness scale
#define _CV_CAMERA_VIEW 6

// w is sky flash strength
#define _CV_ENTITY_VIEW 7

// framebuffer width (pixels)
// framebuffer height (pixels)
// framebuffer width / height
// normalized screen brightness - game setting
#define _CV_VIEW_PARAMS 8

// xy = raw block/sky
// zw = smoothed block/sky
#define _CV_EYE_BRIGHTNESS 9

// w is EMPTY spare slot for now
#define _CV_EYE_POSITION 10

// w is sky rotation in radians
#define _CV_SKYLIGHT_VECTOR 11

#define _CV_FOG_COLOR 12

// rgb: skylight color modified for atmospheric effects
// a: skylight transition smoothing factor 0-1
#define _CV_ATMOSPEHRIC_COLOR 13

// rgb: raw skylight color
// a: skylight illuminance in lux
#define _CV_SKYLIGHT_COLOR 14

// 15 - 18 reserved for cascades 0-3
#define _CV_SHADOW_CENTER 15

// x = fog start
// y = fog end
// z = held light inner angle
// w = held light outer angle
#define _CV_RENDER_INFO 19

// x = rain strength
// y = thunder strength
// z = smoothed rain strength
// w = smoothed thunder strength
#define _CV_WEATHER 20

// UINT ARRAY
#define _CV_RENDER_FRAMES 0
#define _CV_LIGHT_POINTER_EXTENT 1
#define _CV_LIGHT_DATA_FIRST_ROW 2

#define _CV_FLAG_HAS_SKYLIGHT 0
#define _CV_FLAG_IS_OVERWORLD 1
#define _CV_FLAG_IS_NETHER 2
#define _CV_FLAG_IS_END 3
#define _CV_FLAG_IS_RAINING 4
#define _CV_FLAG_IS_THUNDERING 5
#define _CV_FLAG_IS_SKY_DARKENED 6

#define _CV_WORLD_FLAGS_INDEX 0
#define _CV_PLAYER_FLAGS_INDEX 1
#define _CV_CONDITION_FLAGS_START 2

// update each frame
uniform vec4[32] _cvu_world;
uniform uint[3] _cvu_world_uint;
uniform uint[4] _cvu_flags;

#define _CV_MODEL_TO_WORLD 0
#define _CV_MODEL_TO_CAMERA 1

// updated each invocation as needed
uniform vec4[2] _cvu_model_origin;
uniform int _cvu_model_origin_type;
uniform mat3 _cvu_normal_model_matrix;
uniform vec2 _cvu_fog_info;

#define _CV_MAT_VIEW 0
#define _CV_MAT_VIEW_INVERSE 1
#define _CV_MAT_VIEW_LAST 2
#define _CV_MAT_PROJ 3
#define _CV_MAT_PROJ_INVERSE 4
#define _CV_MAT_PROJ_LAST 5
#define _CV_MAT_VIEW_PROJ 6
#define _CV_MAT_VIEW_PROJ_INVERSE 7
#define _CV_MAT_VIEW_PROJ_LAST 8
#define _CV_MAT_SHADOW_VIEW 9
#define _CV_MAT_SHADOW_VIEW_INVERSE 10
// base index of cascades 0-3
#define _CV_MAT_SHADOW_PROJ_0 11
// base index of cascades 0-3
#define _CV_MAT_SHADOW_VIEW_PROJ_0 15
#define _CV_MAT_CLEAN_PROJ 19
#define _CV_MAT_CLEAN_PROJ_INVERSE 20
#define _CV_MAT_CLEAN_PROJ_LAST 21
#define _CV_MAT_CLEAN_VIEW_PROJ 22
#define _CV_MAT_CLEAN_VIEW_PROJ_INVERSE 23
#define _CV_MAT_CLEAN_VIEW_PROJ_LAST 24

uniform mat4[25] _cvu_matrix;

uniform mat4 _cvu_guiViewProjMatrix;

#define _cv_bitValue(bits, bitIndex) int((bits >> bitIndex) & 1u)
#define _cv_testConditioni(conditionIndex) _cv_bitValue(_cvu_flags[_CV_CONDITION_FLAGS_START + (conditionIndex >> 5)], (conditionIndex & 31))
#define _cv_testCondition(conditionIndex) (_cv_testConditioni(conditionIndex) == 1)
