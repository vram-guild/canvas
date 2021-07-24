#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/
#ifdef VERTEX_SHADER

// Same as default but region is looked up based on a vertex attribute.
// This avoid a uniform update per draw call.
#ifdef _CV_VERTEX_REGION

#define SECTOR_X_ORIGIN_INDEX 182
#define SECTOR_Z_ORIGIN_INDEX 183

flat out vec4 _cv_modelToWorld;
flat out vec4 _cv_modelToCamera;

uniform int[256] _cvu_sectors_int;

in int in_region;
in vec3 in_modelpos;
in ivec3 in_blockpos;
in vec4 in_color;
in vec2 in_uv;
in vec2 in_lightmap;
in int in_material;
in vec3 in_normal;
in float in_ao;

vec3 in_vertex;

void _cv_prepareForVertex() {
	int packedSector = _cvu_sectors_int[in_region >> 1];
	packedSector = (in_region & 1) == 1 ? ((packedSector >> 16) & 0xFFFF) : (packedSector & 0xFFFF);

	int originX = _cvu_sectors_int[SECTOR_X_ORIGIN_INDEX] + ((packedSector & 0xF) - 6) * 128;
	int originY = ((packedSector >> 4) & 0xF) * 128 - 64;
	int originZ = _cvu_sectors_int[SECTOR_Z_ORIGIN_INDEX] + (((packedSector >> 8) & 0xF) - 6) * 128;

	_cv_modelToWorld = vec4(originX, originY, originZ, 0.0);
	_cv_modelToCamera = vec4(_cv_modelToWorld.xyz - _cvu_world[_CV_CAMERA_POS].xyz, 0.0);
	in_vertex = in_modelpos + in_blockpos - 63;
}
#endif

#ifdef _CV_VERTEX_DEFAULT
in vec3 in_vertex;
in vec4 in_color;
in vec2 in_uv;
in vec2 in_lightmap;
in int in_material;
in vec3 in_normal;
in float in_ao;

void _cv_prepareForVertex() { }
#endif

#endif
