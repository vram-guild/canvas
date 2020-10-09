#include canvas:shaders/internal/world.glsl

/******************************************************
  frex:shaders/api/camera.glsl

  Utilities for querying camera information
******************************************************/

/*
 *  The view vector of the current camera, expressed in a normalised vector
 */
vec3 frx_cameraView() {
	return vec3(_cvu_world[_CV_CAMERA_VIEW], _cvu_world[_CV_CAMERA_VIEW + 1], _cvu_world[_CV_CAMERA_VIEW + 2]);
}

/*
 *  The view vector of the current entity focused by the camera, expressed in a normalised vector
 */
vec3 frx_entityView() {
	return vec3(_cvu_world[_CV_ENTITY_VIEW], _cvu_world[_CV_ENTITY_VIEW + 1], _cvu_world[_CV_ENTITY_VIEW + 2]);
}
