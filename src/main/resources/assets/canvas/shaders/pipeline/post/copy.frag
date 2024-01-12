#include frex:shaders/api/header.glsl
#include canvas:shaders/pipeline/pipeline.glsl

/******************************************************
  canvas:shaders/pipeline/post/color.frag
******************************************************/
uniform sampler2D _cvu_input;

in vec2 _cvv_texcoord;
out vec4 fragColor;

void main() {
	fragColor = texture(_cvu_input, _cvv_texcoord);
}
