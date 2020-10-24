/******************************************************
  canvas:shaders/internal/vertex.glsl
******************************************************/

#define _CV_SPRITE_INFO_TEXTURE_SIZE 0
#define _CV_ATLAS_WIDTH 1
#define _CV_ATLAS_HEIGHT 2

uniform float[4] _cvu_material;

vec2 _cv_textureCoord(vec2 coordIn, int matrixIndex) {
	vec4 temp = gl_TextureMatrix[matrixIndex] * coordIn.xyxy;
	return temp.xy;
}
