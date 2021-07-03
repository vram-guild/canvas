#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

//#define CV_VF

#ifdef VERTEX_SHADER
in vec3 in_vertex;

#ifdef CV_VF
in int in_color_vf;
in int in_uv_vf;
#define in_color texelFetch(_cvu_vfColor, in_color_vf)
#define in_uv texelFetch(_cvu_vfUV, in_uv_vf).rg
#else
in vec4 in_color;
in vec2 in_uv;
#endif

in int in_material;

#ifdef VANILLA_LIGHTING
in vec2 in_lightmap;
in vec3 in_normal;
in float in_ao;
#endif

#endif

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	// TODO: need texture matrix?
	//vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	//return temp.xy;
	return coordIn.xy;
}
