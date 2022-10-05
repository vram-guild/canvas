#include frex:shaders/api/header.glsl
#include abstract:shaders/pipeline/pipeline.glsl
#include frex:shaders/lib/sample.glsl
#include abstract:shaders/pipeline/post/bloom_options.glsl

/******************************************************
  abstract:shaders/pipeline/post/downsample.frag
******************************************************/
uniform sampler2D _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	fragColor = frx_sample13(_cvu_input, _cvv_texcoord, BLOOM_DOWNSAMPLE_DIST_VEC / frxu_size, max(0, frxu_lod - 1));
}
