#include frex:shaders/api/header.glsl
#include frex:shaders/lib/sample.glsl
#include canvas:shaders/pipeline/pipeline.glsl
#include canvas:shaders/pipeline/post/bloom_options.glsl

/******************************************************
  canvas:shaders/pipeline/post/downsample.frag
******************************************************/
uniform sampler2D cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	fragColor = textureLod(cvu_input, _cvv_texcoord, frxu_lod);
}
