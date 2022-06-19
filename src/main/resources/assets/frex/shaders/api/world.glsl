#include frex:shaders/api/view.glsl
#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/flags.glsl

/****************************************************************
 * frex:shaders/api/world.glsl - Canvas Implementation
 ***************************************************************/

#define frx_renderSeconds _cvu_world[_CV_WORLD_TIME].x
#define frx_renderFrames _cvu_world_uint[_CV_RENDER_FRAMES]
#define frx_worldDay _cvu_world[_CV_WORLD_TIME].z
#define frx_worldTime _cvu_world[_CV_WORLD_TIME].y
#define frx_moonSize _cvu_world[_CV_WORLD_TIME].w
#define frx_skyAngleRadians _cvu_world[_CV_SKYLIGHT_VECTOR].w
#define frx_skyLightVector _cvu_world[_CV_SKYLIGHT_VECTOR].xyz
#define frx_skyLightColor _cvu_world[_CV_SKYLIGHT_COLOR].xyz
#define frx_skyLightIlluminance _cvu_world[_CV_SKYLIGHT_COLOR].w
#define frx_skyLightAtmosphericColor _cvu_world[_CV_ATMOSPEHRIC_COLOR].xyz
#define frx_skyLightTransitionFactor _cvu_world[_CV_ATMOSPEHRIC_COLOR].w
#define frx_skyFlashStrength _cvu_world[_CV_ENTITY_VIEW].w
#define frx_ambientIntensity _cvu_world[_CV_AMBIENT_LIGHT].a
#define frx_darknessFactor _cvu_world[_CV_CAMERA_VIEW].w
#define frx_emissiveColor vec4(_cvu_world[_CV_AMBIENT_LIGHT].rgb, 1.0)
#define frx_rainGradient _cvu_world[_CV_WEATHER].x
#define frx_thunderGradient _cvu_world[_CV_WEATHER].y
#define frx_smoothedRainGradient _cvu_world[_CV_WEATHER].z
#define frx_smoothedThunderGradient _cvu_world[_CV_WEATHER].w
#define frx_vanillaClearColor _cvu_world[_CV_CLEAR_COLOR].rgb
#define frx_worldHasSkylight int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 0) & 1u)
#define frx_worldIsOverworld int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 1) & 1u)
#define frx_worldIsNether int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 2) & 1u)
#define frx_worldIsEnd int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 3) & 1u)
#define frx_worldIsRaining int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 4) & 1u)
#define frx_worldIsThundering int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 5) & 1u)
#define frx_worldIsSkyDarkened int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 6) & 1u)
#define frx_worldIsMoonlit int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 21) & 1u)
#define frx_conditionTest(conditionIndex) _cv_testConditioni(conditionIndex)

#define frx_testCondition(conditionIndex) (frx_conditionTest(conditionIndex) == 1) // DEPRECATED - DO NOT USE
#define frx_worldFlag(flag) (frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], flag) == 1.0) // DEPRECATED - DO NOT USE
