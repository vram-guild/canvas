#include canvas:shaders/internal/process/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl
#include frex:shaders/lib/sample.glsl

/******************************************************
  canvas:shaders/internal/process/downsample.frag
******************************************************/
uniform sampler2D _cvu_input;

varying vec2 _cvv_texcoord;

void main() {
	gl_FragData[0] = frx_sample13(_cvu_input, _cvv_texcoord, BLOOM_DOWNSAMPLE_DIST_VEC / _cvu_size, max(0, _cvu_lod - 1));
}
