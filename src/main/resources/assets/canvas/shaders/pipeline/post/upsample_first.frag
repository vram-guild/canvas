#include canvas:shaders/pipeline/post/header.glsl
#include frex:shaders/lib/sample.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/pipeline/post/downsample.frag
******************************************************/
uniform sampler2D cvu_input;

varying vec2 _cvv_texcoord;

void main() {
	gl_FragData[0] = texture2DLod(cvu_input, _cvv_texcoord, _cvu_lod);
}
