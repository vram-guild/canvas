#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

//#define CV_VF

#ifdef VERTEX_SHADER

#ifdef CV_VF

uniform samplerBuffer _cvu_vfColor;
uniform samplerBuffer _cvu_vfUV;
uniform isamplerBuffer _cvu_vfVertex;

#define VERTEX_MULTIPLIER (1.0 / 1048575.0)

in int in_header_vf;
in int in_vertex_vf;
in int in_pad0_vf;
in int in_color_vf;
in int in_uv_vf;
in int in_material;
//#define in_material (in_header_vf & 0xFFFF)
#define in_color texelFetch(_cvu_vfColor, in_color_vf)
#define in_uv texelFetch(_cvu_vfUV, in_uv_vf).rg

vec3 in_vertex;
vec3 in_normal;

void prepareForVertex() {
	ivec4 vfv = texelFetch(_cvu_vfVertex, in_vertex_vf);
	in_vertex = vec3(vfv.xyz) * VERTEX_MULTIPLIER + vec3(in_header_vf & 0xF, (in_header_vf >> 4) & 0xF, (in_header_vf >> 8) & 0xF);
	in_normal = (vec3(vfv.w & 0xFF, (vfv.w >> 8) & 0xFF, (vfv.w >> 16) & 0xFF) - 127.0) / 127.0;
}

#else
in vec3 in_vertex;
in vec4 in_color;
in vec2 in_uv;
in int in_material;
in vec3 in_normal;

void prepareForVertex() { }

#endif


#ifdef VANILLA_LIGHTING
in vec2 in_lightmap;
in float in_ao;
#endif

#endif
