#include frex:shaders/api/header.glsl
#include frex:shaders/lib/sample.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/pipeline/post/downsample.frag
******************************************************/
uniform sampler2D cvu_input;
uniform sampler2D cvu_prior;

varying vec2 _cvv_texcoord;

void main() {
	vec4 prior = frx_sampleTent(cvu_prior, _cvv_texcoord, BLOOM_UPSAMPLE_DIST_VEC / frxu_size, frxu_lod + 1);
	gl_FragData[0] = texture2DLod(cvu_input, _cvv_texcoord, frxu_lod) + prior;
}
