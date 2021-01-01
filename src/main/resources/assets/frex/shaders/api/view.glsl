#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  frex:shaders/api/view.glsl

  Utilities for querying camera and transformations.
******************************************************/

/*
 *  The view vector of the current camera in world space, normalised.
 */
vec3 frx_cameraView() {
	return _cvu_world[_CV_CAMERA_VIEW].xyz;
}

/*
 *  The view vector of the current entity focused by the camera, in world space, normalised.
 */
vec3 frx_entityView() {
	return _cvu_world[_CV_ENTITY_VIEW].xyz;
}

/**
 * Current position of the camera in world space.
 */
vec3 frx_cameraPos() {
	return _cvu_world[_CV_CAMERA_POS].xyz;
}

/**
 * Prior-frame position of the camera in world space.
 */
vec3 frx_lastCameraPos() {
	return _cvu_world[_CV_LAST_CAMERA_POS].xyz;
}

/*
 * World-space coordinates for model space origin in the current invocation.
 * Outside of terrain rendering, this will always be the same as frx_cameraPos().
 * Add this to model-space vertex coordinates to get world coordinates.
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

/*
 * Framebuffer width, in pixels.
 */
float frx_viewWidth() {
	return _cvu_world[_CV_VIEW_PARAMS].x;
}

/*
 * Framebuffer height, in pixels.
 */
float frx_viewHeight() {
	return _cvu_world[_CV_VIEW_PARAMS].y;
}

/*
 * Framebuffer width / height.
 */
float frx_viewAspectRatio() {
	return _cvu_world[_CV_VIEW_PARAMS].z;
}

/*
 * User-configured brightness from game options.
 * Values 0.0 to 1.0, with 1.0 being max brightness.
 */
float frx_viewBrightness() {
	return _cvu_world[_CV_VIEW_PARAMS].w;
}

#define TARGET_SOLID 0
#define TARGET_OUTLINE 1 		// currently not available in managed draws
#define TARGET_TRANSLUCENT 2
#define TARGET_PARTICLES 3
#define TARGET_WEATHER 4		// currently not available in managed draws
#define TARGET_CLOUDS 5			// currently not available in managed draws
#define TARGET_ENTITY 6

/**
 * Indicates which managed render target is being drawn to.
 * Useful if framebuffer configuration differs across targets.
 * One of the TARGET_...  constants above.
 */
int frx_renderTarget() {
	return _cvu_context[_CV_TARGET_INDEX];
}
