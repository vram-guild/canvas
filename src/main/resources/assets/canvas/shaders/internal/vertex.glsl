#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/
#ifdef VERTEX_SHADER

#ifdef _CV_VERTEX_FETCH
flat out vec4 _cv_modelToWorld;
flat out vec4 _cv_modelToCamera;

uniform samplerBuffer _cvu_vfColor;
uniform samplerBuffer _cvu_vfUV;
uniform isamplerBuffer _cvu_vfVertex;
uniform samplerBuffer _cvu_vfLight;
uniform isamplerBuffer _cvu_vfQuads;
uniform isamplerBuffer _cvu_vfRegions;
uniform usamplerBuffer _cvu_vfQuadRegions;

// x is region base index
// y is quad:region map base index
uniform ivec2 _cvu_baseIndex;

int in_material;
vec3 in_vertex;
vec3 in_normal;
vec4 in_color;
vec2 in_uv;
vec2 in_lightmap;
float in_ao;

void _cv_prepareForVertex() {
	int quadID = gl_VertexID / 6;
	int regionIndex = int(texelFetch(_cvu_vfQuadRegions, quadID + _cvu_baseIndex.y).x) + _cvu_baseIndex.x;
	ivec4 region = texelFetch(_cvu_vfRegions, regionIndex);

	_cv_modelToWorld = vec4(region.xyz, 0.0);
	_cv_modelToCamera = vec4(region.xyz - _cvu_world[_CV_CAMERA_POS].xyz, 0.0);

	int v = gl_VertexID - quadID * 6;
	v = v < 3 ? v : ((v - 1) & 3);

	ivec4 q = texelFetch(_cvu_vfQuads, region.w + quadID);
	in_material = (q.x >> 12) & 0xFFFF;

	ivec4 vfv = texelFetch(_cvu_vfVertex, ((q.y & 0xFFFFFF) << 2) + v);
	in_vertex = intBitsToFloat(vfv.xyz) + vec3(q.x & 0xF, (q.x >> 4) & 0xF, (q.x >> 8) & 0xF);
	in_normal = (vec3(vfv.w & 0xFF, (vfv.w >> 8) & 0xFF, (vfv.w >> 16) & 0xFF) - 127.0) / 127.0;

	in_color = texelFetch(_cvu_vfColor, ((q.z & 0xFFFFFF) << 2) + v);

	in_uv = texelFetch(_cvu_vfUV, ((q.w & 0xFFFFFF) << 2) + v).rg;

	int lightIndex = ((q.y >> 24) & 0xFF) | ((q.z >> 16) & 0xFF00) | ((q.w >> 8) & 0xFF0000);
	vec4 light = texelFetch(_cvu_vfLight, (lightIndex << 2) + v);
	in_lightmap = light.rg * 256.0;
	in_ao = light.b;
}
#endif

// Same as default but region is looked up based on a vertex attribute.
// This avoid a uniform update per draw call.
#ifdef _CV_VERTEX_REGION

#define SECTOR_X_ORIGIN_INDEX 182
#define SECTOR_Z_ORIGIN_INDEX 183

flat out vec4 _cv_modelToWorld;
flat out vec4 _cv_modelToCamera;

uniform isamplerBuffer _cvu_vfRegions;
uniform int[] _cvu_sectors_int;

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
	ivec4 region = texelFetch(_cvu_vfRegions, in_region);
	_cv_modelToWorld = vec4(region.xyz, 0.0);
	_cv_modelToCamera = vec4(region.xyz - _cvu_world[_CV_CAMERA_POS].xyz, 0.0);
	in_vertex = in_modelpos + in_blockpos - 119.0;
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
