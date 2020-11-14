/******************************************************
  frex:shaders/lib/face.glsl

  Utilities for deriving facing and face-related attributes.
******************************************************/

#define  FACE_DOWN  0
#define  FACE_UP    1
#define  FACE_NORTH 2
#define  FACE_SOUTH 3
#define  FACE_WEST  4
#define  FACE_EAST  5

const mat3[6] FRX_UV_MATRIX = mat3[6](
mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
mat3(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
mat3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0),
mat3(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0)
);

/*
 * Returns the FACE_ constant most consistent with the
 * provided world-space normal.
 *
 * Will return garbage for normals in screen space.
 */
int frx_face(vec3 normal) {
	vec3 a = abs(normal);
	float m = max(max(a.x, a.y), a.z);

	return a.x == m ? (normal.x > 0 ? FACE_EAST : FACE_WEST)
	: a.y == m ? (normal.y > 0 ? FACE_UP : FACE_DOWN)
	: (normal.z > 0 ? FACE_SOUTH : FACE_NORTH);
}

/*
 * Estimates UV coordinates for a world-space position
 * and world-space normal, assuming texture coordinates
 * are from the 0,0 face corner to the opposite corner.
 *
 * The result is similar to "locked-uv" coordinate mapping
 * in block/item models.
 *
 * Will return garbage for vertex or normals in screen space.
 */
vec2 frx_faceUv(vec3 pos, vec3 normal) {
	mat3 m = FRX_UV_MATRIX[frx_face(normal)];
	vec3 result = m * pos;
	return result.xy;
}

/*
 * Estimates UV coordinates for a world-space position
 * and world-space normal, assuming texture coordinates
 * are from the 0,0 face corner to the opposite corner.
 *
 * The result is similar to "locked-uv" coordinate mapping
 * in block/item models.
 *
 * Will return garbage for vertex or normals in screen space.
 */
vec2 frx_faceUv(vec3 pos, int face) {
	mat3 m = FRX_UV_MATRIX[face];
	vec3 result = m * pos;
	return result.xy;
}
