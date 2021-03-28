#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

#ifdef VERTEX_SHADER
in vec3 in_vertex;
in vec4 in_color;
in vec2 in_uv;
in vec2 in_material;
	#ifdef VANILLA_LIGHTING
in vec4 in_lightmap;
in vec4 in_normal_flags;
	#endif
#endif

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	// TODO: need texture matrix?
	//vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	//return temp.xy;
	return coordIn.xy;
}
