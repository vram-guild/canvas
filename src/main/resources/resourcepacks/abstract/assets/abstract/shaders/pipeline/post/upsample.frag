#include frex:shaders/api/header.glsl
#include frex:shaders/lib/sample.glsl
#include abstract:shaders/pipeline/pipeline.glsl
#include abstract:shaders/pipeline/post/bloom_options.glsl

/******************************************************
  abstract:shaders/pipeline/post/downsample.frag
******************************************************/
uniform sampler2D cvu_input;
uniform sampler2D cvu_prior;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	vec4 prior = frx_sampleTent(cvu_prior, _cvv_texcoord, BLOOM_UPSAMPLE_DIST_VEC / frxu_size, frxu_lod + 1);
	fragColor = textureLod(cvu_input, _cvv_texcoord, frxu_lod) + prior;
}
