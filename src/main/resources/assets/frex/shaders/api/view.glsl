#include canvas:shaders/internal/world.glsl
#include canvas:shaders/internal/program.glsl

/******************************************************
  frex:shaders/api/view.glsl

  Utilities for querying camera and transformations.
******************************************************/

// Coordinate system terminology used here includes:
//
// Model Space
// Block-aligned coordinates without camera rotation, relative
// to an origin that may vary. Translate to camera or world space
// with frx_modelToWorld() or frx_modelToCamera().
//
// Camera Space
// Block-aligned coordinates without camera rotation, relative
// to the camera position. This is the coordinate system for
// inbound coordinates other than terrain and overlays.
// Translate to world space by adding frx_cameraPos().
// Translate to view space with frx_viewMatrix().
//
// World Space
// Block-algined coordinates in absolute MC world space.
// These coordinates will suffer the limits of floating point
// precision when the player is far from the world origin and
// thus they are impractical for some use cases.
// Translate to camera space by subtracting frx_cameraPos().
//
// View Space
// Like camera space, but view is rotates so that the Z axis extends
// in the direction of the camera view.
// Translate to screen space with frx_projectionMatrix().
//
// Screen Space
// 2D projection of the scene with depth.


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

/**
 * Translation from inbound model coordinates to world space. Conventionally
 * this is handled with a matrix, but because inbound coordinates outside of
 * overlay and GUI rendering are always world-aligned, this can avoid a
 * matrix multiplication.
 *
 * When frx_modelOriginType() == MODEL_ORIGIN_CAMERA, inbound coordinates are
 * relative to the camera position and this will equal frx_cameraPos().
 *
 * In overlay rendering, when frx_modelOriginType() == MODEL_ORIGIN_SCREEN, this
 * will always be zero.  The fourth component is always zero and included for
 * ease of use.
 */
vec4 frx_modelToWorld() {
	return _cvu_model_origin[_CV_MODEL_TO_WORLD];
}

/*
 * DEPRECATED use frx_modelToWorld()

 * World-space coordinates for model space origin in the current invocation.
 * Outside of terrain rendering, this will always be the same as frx_cameraPos().
 * Add this to model-space vertex coordinates to get world coordinates.
 */
vec3 frx_modelOriginWorldPos() {
	return frx_modelToWorld().xyz;
}

/**
 * Translation from in-bound model coordinates to view space. Conventionally
 * this is handled with a matrix, but because inbound coordinates outside of
 * overlay and GUI rendering are always world-algined, this can avoid a
 * matrix multiplication.
 *
 * When frx_modelOriginType() == MODEL_ORIGIN_CAMERA, inbound coordinates are
 * already relative to the camera position and this will always be zero.
 *
 * In overlay rendering, when frx_modelOriginType() == MODEL_ORIGIN_SCREEN, this
 * will always be zero.  The fourth component is always zero and included for
 * ease of use.
 */
vec4 frx_modelToCamera() {
	return _cvu_model_origin[_CV_MODEL_TO_CAMERA];
}

/*
 * Vertex coordinates in frx_startVertex are relative to the camera position.
 * Coordinates and normals are unrotated.
 * frx_modelOriginWorldPos() returns camera position.
 */
#define MODEL_ORIGIN_CAMERA 0

/*
 * Vertex coordinates in frx_startVertex are relative to the origin of a
 * "cluster" of world render regions.
 * Coordinates and normals are unrotated.
 * frx_modelOriginWorldPos() returns the cluster origin.
 */
#define MODEL_ORIGIN_REGION 1

/*
 * Vertex coordinates are relative to the screen.  No transforms should be applied.
 * Intended for Hand//GUI rendering.
 */
#define MODEL_ORIGIN_SCREEN 2

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

/*
 * True when rendering hand.
 */
bool frx_isHand() {
    return frx_bitValue(uint(_cvu_context[_CV_CONTEXT_FLAGS]), _CV_CONTEXT_FLAG_HAND) == 1;
}

/*
 * True when rendering to GUI.
 */
bool frx_isGui() {
	return _cvu_model_origin_type == MODEL_ORIGIN_SCREEN;
}

mat4 frx_guiViewProjectionMatrix() {
	return _cvu_guiViewProjMatrix;
}

/**
 * Converts camera/world space normals to view space.
 * Incoming vertex normals are always in camera/world space.
 */
mat3 frx_normalModelMatrix() {
	return _cvu_normal_model_matrix;
}

mat4 frx_viewMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW];
}

mat4 frx_inverseViewMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW_INVERSE];
}

mat4 frx_lastViewMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW_LAST];
}

mat4 frx_projectionMatrix() {
	return _cvu_matrix[_CV_MAT_PROJ];
}

mat4 frx_lastProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_PROJ_LAST];
}

mat4 frx_inverseProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_PROJ_INVERSE];
}

mat4 frx_viewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW_PROJ];
}

mat4 frx_inverseViewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW_PROJ_INVERSE];
}

mat4 frx_lastViewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_VIEW_PROJ_LAST];
}

// No view bobbing or other effects that alter projection
mat4 frx_cleanProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_PROJ];
}

// No view bobbing or other effects that alter projection
mat4 frx_lastCleanProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_PROJ_LAST];
}

// No view bobbing or other effects that alter projection
mat4 frx_inverseCleanProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_PROJ_INVERSE];
}

// No view bobbing or other effects that alter projection
mat4 frx_cleanViewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ];
}

// No view bobbing or other effects that alter projection
mat4 frx_inverseCleanViewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ_INVERSE];
}

// No view bobbing or other effects that alter projection
mat4 frx_lastCleanViewProjectionMatrix() {
	return _cvu_matrix[_CV_MAT_CLEAN_VIEW_PROJ_LAST];
}

mat4 frx_shadowViewMatrix() {
	return _cvu_matrix[_CV_MAT_SHADOW_VIEW];
}

mat4 frx_inverseShadowViewMatrix() {
	return _cvu_matrix[_CV_MAT_SHADOW_VIEW_INVERSE];
}

/**
 * Orthogonal projection matrix on light space for given cascade index 0-3.
 */
mat4 frx_shadowProjectionMatrix(int index) {
	return _cvu_matrix[_CV_MAT_SHADOW_PROJ_0 + index];
}

/**
 * Combined lightspace view and orthogonal projection for given cascade index 0-3.
 */
mat4 frx_shadowViewProjectionMatrix(int index) {
	return _cvu_matrix[_CV_MAT_SHADOW_VIEW_PROJ_0 + index];
}

/**
 * Center and radius of other projection in light space for given cascade index 0-3.
 */
vec4 frx_shadowCenter(int index) {
	return _cvu_world[_CV_SHADOW_CENTER + index];
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

/**
 * Currently configured terrain view distance, in blocks.
 */
float frx_viewDistance() {
	return _cvu_world[_CV_CLEAR_COLOR].w;
}

// Tokens accepted in frx_viewFlag
#define FRX_CAMERA_IN_FLUID 22
#define FRX_CAMERA_IN_WATER 23
#define FRX_CAMERA_IN_LAVA 24

/*
 * Accepts one of the tokens defined above.  Note that different implementations
 * could define different numeric token values - always use the preprocessor token.
 */
bool frx_viewFlag(int flag) {
	return frx_bitValue(_cvu_flags[_CV_WORLD_FLAGS_INDEX], flag) == 1;
}
