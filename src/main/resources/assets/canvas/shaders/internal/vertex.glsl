#include canvas:shaders/internal/context.glsl

/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

#define _CV_SPRITE_INFO_TEXTURE_SIZE 0
#define _CV_ATLAS_WIDTH 1
#define _CV_ATLAS_HEIGHT 2

#ifdef VERTEX_SHADER
attribute vec4 in_color;
attribute vec2 in_uv;
attribute vec2 in_material;
attribute vec4 in_lightmap;
attribute vec4 in_normal_flags;
#endif

uniform float[4] _cvu_atlas;

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	return temp.xy;
}
