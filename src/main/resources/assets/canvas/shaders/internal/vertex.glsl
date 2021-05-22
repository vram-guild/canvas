#include frex:shaders/api/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

#ifdef VERTEX_SHADER
attribute vec4 in_color;
attribute vec2 in_uv;
attribute vec2 in_material;
	#ifdef VANILLA_LIGHTING
attribute vec4 in_lightmap;
attribute vec4 in_normal_flags;
	#endif
#endif

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	return temp.xy;
}
