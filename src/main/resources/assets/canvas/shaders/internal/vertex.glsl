#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/
#ifdef VERTEX_SHADER

// Same as default but region is looked up based on a vertex attribute.
// This avoid a uniform update per draw call.
#ifdef _CV_VERTEX_TERRAIN

uniform int[182] _cvu_sectors_int;

// High bits store sign for normal and tangent vector z components
in ivec4 in_region;
in ivec4 in_blockpos_ao;
in vec4 in_color;
in vec2 in_uv;
in vec2 in_lightmap;
in int in_material;
// only x and y components, must derive Z
in vec4 in_normal_tangent;

vec3 in_vertex;
float in_ao;
vec3 in_normal;

void _cv_prepareForVertex() {
	// Mask out the bits for vector signs
	int packedSector = _cvu_sectors_int[(in_region.x & 0x3FFF) >> 1];
	packedSector = (in_region.x & 1) == 1 ? ((packedSector >> 16) & 0xFFFF) : (packedSector & 0xFFFF);

	// These are relative to the sector origin, which will be near the camera position
	vec3 origin = vec3(((packedSector & 0xF) - 5) * 128, ((packedSector >> 4) & 0xF) * 128 - 64, (((packedSector >> 8) & 0xF) - 5) * 128);

	// Add intra-sector block pos and fractional block pos
	in_vertex = origin + in_region.yzw / 65535.0 + in_blockpos_ao.xyz - 63;
	
	float normalSign = 1.0 - ((in_region.x >> 14) & 2);
	vec2 normXY2 = in_normal_tangent.xy * in_normal_tangent.xy;
	in_normal = vec3(in_normal_tangent.xy, normalSign * sqrt(clamp(1.0 - normXY2.x - normXY2.y, 0.0, 1.0)));
	
	in_ao = in_blockpos_ao.w * (1.0 / 255.0);
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
