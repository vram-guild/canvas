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

// w is rain strength
#define _CV_CAMERA_VIEW 6

// w is smoothed rain strength
#define _CV_ENTITY_VIEW 7

// framebuffer width (pixels)
// framebuffer height (pixels)
// framebuffer width / height
// normalized screen brightness - game setting
#define _CV_VIEW_PARAMS 8

// xy = raw block/sky
// zw = smoothed block/sky
#define _CV_EYE_BRIGHTNESS 9

// w is thunder strength
#define _CV_EYE_POSITION 10

// w is sky rotation in radians
#define _CV_SKYLIGHT_VECTOR 11

// w is always zero
#define _CV_CAMERA_TO_SKYLIGHT 12

// x: skylight strength 0-1
#define _CV_THINGS_AND_STUFF 13

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
uniform vec4[16] _cvu_world;
uniform uint[4] _cvu_flags;

#define _CV_MODEL_TO_WORLD 0
#define _CV_MODEL_TO_CAMERA 1

// updated each invocation as needed
uniform vec4[2] _cvu_model_origin;
uniform int _cvu_model_origin_type;
uniform mat3 _cvu_normal_model_matrix;
uniform int _cvu_fog_mode;

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
#define _CV_MAT_SHADOW_PROJ 11
#define _CV_MAT_SHADOW_PROJ_INVERSE 12
#define _CV_MAT_SHADOW_VIEW_PROJ 13
#define _CV_MAT_SHADOW_VIEW_PROJ_INVERSE 14

uniform mat4[15] _cvu_matrix;

bool _cv_testCondition(int conditionIndex) {
	return frx_bitValue(_cvu_flags[_CV_CONDITION_FLAGS_START + (conditionIndex >> 5)], conditionIndex & 31) == 1.0;
}
