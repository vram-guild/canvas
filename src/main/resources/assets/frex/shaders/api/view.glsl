#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  frex:shaders/api/view.glsl

  Utilities for querying camera and transformations.
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

/*
 * World coordinates for model space origin in the current invocation.
 * Add this to vertex position to get world position.
 */
vec3 frx_modelOriginWorldPos() {
	return _cvu_model_origin;
}

/*
 * Vertex coordinates are relative to the camera and include model transformations
 * as well as camera rotation and translation via MatrixStack.
 * The GL view matrix will be the identity matrix. (the default state in world render)
 * Used for most per-frame renders (entities, block entities, etc.)
 */
#define MODEL_ORIGIN_ENTITY 0

/*
 * Vertex coordinates are relative to the camera and include model translation, scaling
 * and billboard rotation plus camera translation via matrix stack but not camera rotation.
 * The GL view matrix will include camera rotation.
 * Used for particle rendering.
 */
#define MODEL_ORIGIN_PARTICLE 1

/*
 * Vertex coordinate are raw model coordinates.
 * Will need a view matrix update per draw.
 * Currently not used.
 */
#define MODEL_ORIGIN_MODEL 2

/*
 * Vertex coordinates are relative to a world region and
 * include all model transformations.
 * GL view matrix must be updated for both camera rotation and offset.
 * Used in terrain rendering. Canvas regions may be 16x16 or 256x256.
 */
#define MODEL_ORIGIN_REGION 3

/*
 * Vertex coordinates are relative to the screen.
 * Intended for GUI rendering.
 * Currently not used.
 */
#define MODEL_ORIGIN_SCREEN 4

/**
 * Describes how vertex coordinates relate to world and camera geometry.
 * Will be one of the MODEL_ORIGIN_ constants defined above.
 *
 * Except as noted, GL state is always assumed to have the projection
 * matrix set and view matrix set to identity. Some types have additional
 * view matrix transformation.
 */
int frx_modelOriginType() {
	return _cvu_model_origin_type;
}

/**
 * Converts world space normals to coordinate space of incoming vertex data.
 * Entity render, for example, has camera rotation already baked in to
 * vertex data and so a pure world normal vector can be transformed
 * with this to be consistent with vertex normals.
 */
mat3 frx_normalModelMatrix() {
	return _cvu_normal_model_matrix;
}

/*
 * True when rendering to GUI.
 */
bool frx_isGui() {
	return _cv_isGui() == 1.0;
}
