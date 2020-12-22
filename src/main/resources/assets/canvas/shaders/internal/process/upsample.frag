#include canvas:shaders/internal/process/header.glsl
#include frex:shaders/lib/sample.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/internal/process/downsample.frag
******************************************************/
uniform sampler2D cvu_input;
uniform sampler2D cvu_prior;

varying vec2 _cvv_texcoord;

void main() {
	vec4 prior = frx_sampleTent(cvu_prior, _cvv_texcoord, BLOOM_UPSAMPLE_DIST_VEC / _cvu_size, _cvu_lod + 1);
	gl_FragData[0] = texture2DLod(cvu_input, _cvv_texcoord, _cvu_lod) + prior;
}
