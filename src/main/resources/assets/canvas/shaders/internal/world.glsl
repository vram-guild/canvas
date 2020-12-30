#include frex:shaders/lib/bitwise.glsl

/******************************************************
  canvas:shaders/internal/world.glsl
******************************************************/

#define _CV_WORLD_EFFECT_MODIFIER 0
#define _CV_RENDER_SECONDS 1
#define _CV_AMBIENT_INTENSITY 2
#define _CV_MOON_SIZE 3
#define _CV_WORLD_TIME 4
#define _CV_WORLD_DAYS 5
#define _CV_UNUSED_A 6
#define _CV_UNUSED_B 7
#define _CV_EMISSIVE_COLOR_RED 8
#define _CV_EMISSIVE_COLOR_GREEN 9
#define _CV_EMISSIVE_COLOR_BLUE 10
#define _CV_HELD_LIGHT_RED 11
#define _CV_HELD_LIGHT_GREEN 12
#define _CV_HELD_LIGHT_BLUE 13
#define _CV_HELD_LIGHT_INTENSITY 14
#define _CV_RAIN_GRADIENT 15
#define _CV_CAMERA_VIEW 16 // 3 elements wide
#define _CV_ENTITY_VIEW 19 // 3 elements wide

#define _CV_FLAG0_NIGHT_VISTION_ACTIVE  0
#define _CV_FLAG0_HAS_SKYLIGHT          1
#define _CV_FLAG0_IS_OVERWORLD          2
#define _CV_FLAG0_IS_NETHER            	3
#define _CV_FLAG0_IS_END                4
#define _CV_FLAG0_IS_RAINING            5
#define _CV_FLAG0_IS_THUNDERING        	6
#define _CV_FLAG0_IS_SKY_DARKENED      	7

uniform float[22] _cvu_world;
uniform uint _cvu_world_flags;
uniform uint[2] _cvu_condition_flags;
uniform vec3 _cvu_model_origin;
uniform int _cvu_model_origin_type;
uniform mat3 _cvu_normal_model_matrix;
uniform int _cvu_fog_mode;

bool _cv_testCondition(int conditionIndex) {
	return frx_bitValue(_cvu_condition_flags[conditionIndex >> 5], conditionIndex & 31) == 1.0;
}
