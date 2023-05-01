#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl

/******************************************************
  abstract:shaders/pipeline/post/copy_lod.frag
******************************************************/
uniform sampler2DArray _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	fragColor = textureLod(_cvu_input, vec3(_cvv_texcoord, frxu_layer), frxu_lod);
}
