#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/program.glsl

/****************************************************************
 * frex:shaders/api/view.glsl - Canvas Implementation
 ***************************************************************/

#define frx_cameraView _cvu_world[_CV_CAMERA_VIEW].xyz
#define frx_entityView _cvu_world[_CV_ENTITY_VIEW].xyz
#define frx_cameraPos _cvu_world[_CV_CAMERA_POS].xyz
#define frx_lastCameraPos _cvu_world[_CV_LAST_CAMERA_POS].xyz
#define frx_modelToWorld _cvu_model_origin[_CV_MODEL_TO_WORLD]
#define frx_modelToCamera _cvu_model_origin[_CV_MODEL_TO_CAMERA]
#define frx_modelOriginCamera (_cvu_model_origin_type == 0)
#define frx_modelOriginRegion (_cvu_model_origin_type == 1)
#define frx_modelOriginScreen (_cvu_model_origin_type == 2)
#define frx_isHand (((_cvu_context[_CV_CONTEXT_FLAGS] >> _CV_CONTEXT_FLAG_HAND) & 1) == 1)
#define frx_isGui frx_modelOriginScreen
#define frx_guiViewProjectionMatrix _cvu_guiViewProjMatrix
#define frx_normalModelMatrix _cvu_normal_model_matrix
#define frx_viewMatrix _cvu_matrix[_CV_MAT_VIEW]
#define frx_inverseViewMatrix _cvu_matrix[_CV_MAT_VIEW_INVERSE]
#define frx_lastViewMatrix _cvu_matrix[_CV_MAT_VIEW_LAST]
#define frx_projectionMatrix _cvu_matrix[_CV_MAT_PROJ]
#define frx_lastProjectionMatrix _cvu_matrix[_CV_MAT_PROJ_LAST]
#define frx_inverseProjectionMatrix _cvu_matrix[_CV_MAT_PROJ_INVERSE]
#define frx_viewProjectionMatrix _cvu_matrix[_CV_MAT_VIEW_PROJ]
#define frx_inverseViewProjectionMatrix _cvu_matrix[_CV_MAT_VIEW_PROJ_INVERSE]
#define frx_lastViewProjectionMatrix _cvu_matrix[_CV_MAT_VIEW_PROJ_LAST]
#define frx_cleanProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_PROJ]
#define frx_lastCleanProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_PROJ_LAST]
#define frx_inverseCleanProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_PROJ_INVERSE]
#define frx_cleanViewProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ]
#define frx_inverseCleanViewProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ_INVERSE]
#define frx_lastCleanViewProjectionMatrix _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ_LAST]
#define frx_shadowViewMatrix _cvu_matrix[_CV_MAT_SHADOW_VIEW]
#define frx_inverseShadowViewMatrix _cvu_matrix[_CV_MAT_SHADOW_VIEW_INVERSE]
#define frx_shadowProjectionMatrix(index) (_cvu_matrix[_CV_MAT_SHADOW_PROJ_0 + index])
#define frx_shadowViewProjectionMatrix(index) (_cvu_matrix[_CV_MAT_SHADOW_VIEW_PROJ_0 + index])
#define frx_shadowCenter(index) (_cvu_world[_CV_SHADOW_CENTER + index])
#define frx_viewWidth _cvu_world[_CV_VIEW_PARAMS].x
#define frx_viewHeight _cvu_world[_CV_VIEW_PARAMS].y
#define frx_viewAspectRatio _cvu_world[_CV_VIEW_PARAMS].z
#define frx_viewBrightness _cvu_world[_CV_VIEW_PARAMS].w
#define frx_renderTargetSolid (_cvu_context[_CV_TARGET_INDEX] == 0)
#define frx_renderTargetTranslucent (_cvu_context[_CV_TARGET_INDEX] == 2)
#define frx_renderTargetParticles (_cvu_context[_CV_TARGET_INDEX] == 3)
#define frx_renderTargetEntity (_cvu_context[_CV_TARGET_INDEX] == 6)
#define frx_viewDistance _cvu_world[_CV_CLEAR_COLOR].w
#define frx_cameraInFluid int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 22) & 1u)
#define frx_cameraInWater int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 23) & 1u)
#define frx_cameraInLava int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 24) & 1u)
#define frx_cameraInSnow int((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> 25) & 1u)

#define frx_viewFlag(flag) (((_cvu_flags[_CV_WORLD_FLAGS_INDEX] >> flag) & 1u) == 1u) // DEPRECATED - DO NOT USE
